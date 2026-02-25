import axios, {
  type AxiosProgressEvent,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
  type RawAxiosRequestHeaders,
} from 'axios'
import { message } from 'antd'
import { useAuthStore } from '@/store/auth-store'
import { SIGN_SKIP_PATHS } from '@/lib/constants'
import { signRequest } from '@/lib/request-signature'
import type { ResponseVO } from '@/types'

const contentTypeForm = 'application/x-www-form-urlencoded;charset=UTF-8'
const contentTypeJson = 'application/json;charset=UTF-8'

let isRefreshing = false
let refreshSubscribers: ((token: string) => void)[] = []
const pendingControllers = new Map<string, AbortController>()

interface RequestConfig extends InternalAxiosRequestConfig {
  showError?: boolean
  errorCallback?: (errorMsg: string) => void
  skipAuthRefresh?: boolean
  uploadProgressCallback?: (event: AxiosProgressEvent) => void
  dataType?: 'json' | 'file' | 'form'
  requireSignature?: boolean
  tenantId?: string
  requestKey?: string
  _retry?: boolean
}

interface RequestOptions {
  path: string
  params?: unknown
  dataType?: 'json' | 'file' | 'form'
  showError?: boolean
  errorCallback?: (errorMsg: string) => void
  uploadProgressCallback?: (event: AxiosProgressEvent) => void
  responseType?: AxiosRequestConfig['responseType']
  skipAuthRefresh?: boolean
  requireSignature?: boolean
  tenantId?: string
  requestKey?: string
  cancelPrevious?: boolean
}

interface ErrorPayload {
  showError?: boolean
  msg?: string
  response?: { status?: number }
  config?: RequestConfig
  code?: string
}

const toErrorPayload = (error: unknown): ErrorPayload => {
  if (typeof error === 'object' && error !== null) {
    return error as ErrorPayload
  }
  return {}
}

const normalizePath = (path: string): string => {
  if (!path) {
    return '/api'
  }
  if (/^https?:\/\//i.test(path)) {
    try {
      return new URL(path).pathname
    } catch {
      return '/api'
    }
  }
  const normalized = path.startsWith('/') ? path : `/${path}`
  if (normalized.startsWith('/api/')) {
    return normalized
  }
  return `/api${normalized}`
}

const shouldSkipSignature = (path: string): boolean => {
  return SIGN_SKIP_PATHS.some((prefix) => normalizePath(path).startsWith(`/api${prefix}`.replace('/api/api', '/api')))
}

const resolveTenantId = (tenantId?: string): string => {
  if (tenantId) {
    return tenantId
  }
  const storeTenant = useAuthStore.getState().user?.tenantId
  const sessionTenant = sessionStorage.getItem('tenantId')
  return storeTenant || sessionTenant || 'default'
}

const resolveRequestKey = (path: string, requestKey?: string): string => {
  if (requestKey && requestKey.trim()) {
    return requestKey.trim()
  }
  return normalizePath(path)
}

const attachAbortController = (path: string, requestKey?: string, cancelPrevious = false) => {
  const resolvedKey = resolveRequestKey(path, requestKey)
  if (cancelPrevious) {
    pendingControllers.get(resolvedKey)?.abort()
  }
  const controller = new AbortController()
  pendingControllers.set(resolvedKey, controller)
  return { requestKey: resolvedKey, signal: controller.signal }
}

const clearAbortController = (requestKey: string, signal: AbortSignal) => {
  const current = pendingControllers.get(requestKey)
  if (current?.signal === signal) {
    pendingControllers.delete(requestKey)
  }
}

const statusMessageMap: Record<number, string> = {
  400: '请求参数错误，请刷新后重试',
  401: '当前登录状态无效，请重新登录',
  403: '当前账号暂无该操作权限',
  404: '请求资源不存在',
  408: '请求超时，请稍后重试',
  429: '请求过于频繁，请稍后再试',
  500: '服务暂时不可用，请稍后重试',
  502: '网关异常，请稍后重试',
  503: '服务维护中，请稍后重试',
  504: '服务响应超时，请稍后重试',
}

const resolveHttpErrorMessage = (status?: number): string => {
  if (!status) {
    return '网络异常，请稍后重试'
  }
  return statusMessageMap[status] || '请求失败，请稍后重试'
}

const instance = axios.create({
  baseURL: '/api',
  timeout: 20000,
  withCredentials: true,
})

const rawInstance = axios.create({
  baseURL: '/api',
  timeout: 20000,
  withCredentials: true,
})

const subscribeTokenRefresh = (callback: (token: string) => void) => {
  refreshSubscribers.push(callback)
}

const onTokenRefreshed = (token: string) => {
  refreshSubscribers.forEach((callback) => callback(token))
  refreshSubscribers = []
}

const toQueryString = (params: Record<string, unknown>): string => {
  const data = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    data.append(key, value instanceof Blob ? '' : String(value ?? ''))
  })
  return data.toString()
}

const jumpToLogin = () => {
  const currentPath = `${window.location.pathname}${window.location.search}`
  const redirect = encodeURIComponent(currentPath)
  window.location.href = `/login?redirectUrl=${redirect}`
}

const refreshTokenRequest = async (): Promise<string | null> => {
  const auth = useAuthStore.getState()
  const refreshToken = auth.user?.refreshToken
  if (!refreshToken) {
    return null
  }
  try {
    const response = await rawInstance.post<ResponseVO<{ token: string; refreshToken?: string }>>(
      '/refreshToken',
      toQueryString({ refreshToken }),
      {
        headers: {
          'Content-Type': contentTypeForm,
          'X-Tenant-Id': resolveTenantId(),
        },
      }
    )

    if (response.data.code === 200 && response.data.data?.token) {
      const currentUser = useAuthStore.getState().user
      if (currentUser) {
        useAuthStore.getState().setUser({
          ...currentUser,
          token: response.data.data.token,
          refreshToken: response.data.data.refreshToken || refreshToken,
        })
      }
      return response.data.data.token
    }
  } catch {
    return null
  }
  return null
}

instance.interceptors.request.use(async (requestConfig) => {
  const config = requestConfig as RequestConfig
  config.headers = config.headers || {}

  config.headers['X-Tenant-Id'] = resolveTenantId(config.tenantId)
  const token = useAuthStore.getState().user?.token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  const path = normalizePath(config.url || '')
  const shouldSign = config.requireSignature !== false && !shouldSkipSignature(path)
  if (shouldSign) {
    const signatureHeaders = await signRequest(config.method || 'POST', path, '')
    Object.assign(config.headers, signatureHeaders)
  }

  return config
})

instance.interceptors.response.use(
  async (response: AxiosResponse) => {
    const config = response.config as RequestConfig

    if (config.responseType === 'blob' || config.responseType === 'arraybuffer') {
      return response.data
    }

    const payload = response.data as ResponseVO
    if (payload.code === 200) {
      return payload
    }

    if (payload.code === 901 && !config.skipAuthRefresh) {
      if (!isRefreshing) {
        isRefreshing = true
        const newToken = await refreshTokenRequest()
        isRefreshing = false
        if (newToken) {
          onTokenRefreshed(newToken)
          config.headers.Authorization = `Bearer ${newToken}`
          return instance(config)
        }
        useAuthStore.getState().clearUser()
        jumpToLogin()
        return Promise.reject({ showError: false, msg: '登录超时' })
      }

      return new Promise((resolve) => {
        subscribeTokenRefresh((token) => {
          config.headers.Authorization = `Bearer ${token}`
          resolve(instance(config))
        })
      })
    }

    const msg = payload.info || '请求失败'
    if (config.errorCallback) {
      config.errorCallback(msg)
    }
    return Promise.reject({ showError: config.showError !== false, msg })
  },
  (error: unknown) => {
    const payload = toErrorPayload(error)
    if (payload.code === 'ERR_CANCELED' || axios.isCancel(error)) {
      return Promise.reject({ showError: false, msg: '请求已取消', code: 'ERR_CANCELED' })
    }
    if (payload.code === 'ECONNABORTED') {
      return Promise.reject({ showError: true, msg: '请求超时，请检查网络后重试', code: payload.code })
    }
    return Promise.reject({
      showError: true,
      msg: resolveHttpErrorMessage(payload.response?.status),
      code: payload.code,
    })
  }
)

const buildBody = (
  params: unknown,
  dataType: 'json' | 'file' | 'form'
): { body: unknown; contentType: string | null } => {
  if (dataType === 'json') {
    return {
      body: (params as object) || {},
      contentType: contentTypeJson,
    }
  }

  if (dataType === 'file') {
    if (params instanceof FormData) {
      return { body: params, contentType: null }
    }
    const formData = new FormData()
    Object.entries((params as Record<string, unknown>) || {}).forEach(([key, value]) => {
      if (value instanceof Blob) {
        formData.append(key, value)
      } else {
        formData.append(key, String(value ?? ''))
      }
    })
    return { body: formData, contentType: null }
  }

  if (params instanceof URLSearchParams) {
    return { body: params.toString(), contentType: contentTypeForm }
  }

  if (typeof params !== 'object' || params === null) {
    return { body: '', contentType: contentTypeForm }
  }

  return {
    body: toQueryString(params as Record<string, unknown>),
    contentType: contentTypeForm,
  }
}

export const request = async <T = unknown>(options: RequestOptions): Promise<T | null> => {
  const {
    path,
    params,
    dataType = 'form',
    showError = true,
    errorCallback,
    uploadProgressCallback,
    responseType,
    skipAuthRefresh,
    requireSignature,
    tenantId,
    requestKey,
    cancelPrevious = false,
  } = options

  const bodyPayload = buildBody(params, dataType)
  const abortMeta = attachAbortController(path, requestKey, cancelPrevious)
  const headers: RawAxiosRequestHeaders = {
    'X-Requested-with': 'XMLHttpRequest',
  }

  if (bodyPayload.contentType) {
    headers['Content-Type'] = bodyPayload.contentType
  }

  try {
    const response = (await instance.post(path, bodyPayload.body, {
      headers,
      responseType,
      showError,
      errorCallback,
      skipAuthRefresh,
      requireSignature,
      tenantId,
      requestKey: abortMeta.requestKey,
      uploadProgressCallback,
      signal: abortMeta.signal,
    } as unknown as RequestConfig)) as ResponseVO<T> | T

    if (responseType === 'blob' || responseType === 'arraybuffer') {
      return response as T
    }

    return (response as ResponseVO<T>).data
  } catch (error: unknown) {
    const payload = toErrorPayload(error)
    if (payload.code === 'ERR_CANCELED') {
      return null
    }
    if (payload.msg && errorCallback) {
      errorCallback(payload.msg)
    }
    if (showError && payload.msg) {
      message.error(payload.msg)
    }
    return null
  } finally {
    clearAbortController(abortMeta.requestKey, abortMeta.signal)
  }
}

export const requestSuccess = async (options: RequestOptions): Promise<boolean> => {
  const {
    path,
    params,
    dataType = 'form',
    showError = true,
    errorCallback,
    uploadProgressCallback,
    skipAuthRefresh,
    requireSignature,
    tenantId,
    requestKey,
    cancelPrevious = false,
  } = options

  const bodyPayload = buildBody(params, dataType)
  const abortMeta = attachAbortController(path, requestKey, cancelPrevious)
  const headers: RawAxiosRequestHeaders = {
    'X-Requested-with': 'XMLHttpRequest',
  }

  if (bodyPayload.contentType) {
    headers['Content-Type'] = bodyPayload.contentType
  }

  try {
    await instance.post(path, bodyPayload.body, {
      headers,
      showError,
      errorCallback,
      skipAuthRefresh,
      requireSignature,
      tenantId,
      requestKey: abortMeta.requestKey,
      uploadProgressCallback,
      signal: abortMeta.signal,
    } as unknown as RequestConfig)
    return true
  } catch (error: unknown) {
    const payload = toErrorPayload(error)
    if (payload.code === 'ERR_CANCELED') {
      return false
    }
    if (payload.msg && errorCallback) {
      errorCallback(payload.msg)
    }
    if (showError && payload.msg) {
      message.error(payload.msg)
    }
    return false
  } finally {
    clearAbortController(abortMeta.requestKey, abortMeta.signal)
  }
}

export const fetchBlob = async (path: string): Promise<Blob | null> => {
  return request<Blob>({ path, responseType: 'blob', showError: false })
}

export const fetchArrayBuffer = async (path: string): Promise<ArrayBuffer | null> => {
  return request<ArrayBuffer>({ path, responseType: 'arraybuffer', showError: false })
}

export type { AxiosProgressEvent }


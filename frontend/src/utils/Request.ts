import axios, { type InternalAxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElLoading } from 'element-plus'
import router from '@/router'
import Message from '../utils/Message'
import { useUserInfoStore } from '@/stores/userInfoStore'

const contentTypeForm = 'application/x-www-form-urlencoded;charset=UTF-8'
const contentTypeJson = 'application/json'
const responseTypeJson = 'json'

let loadingInstance: any = null
let loadingCount = 0
let loadingTimer: any = null
let isRefreshing = false
let refreshSubscribers: ((token: string) => void)[] = []

// 请求取消相关
const pendingRequests = new Map<string, AbortController>();

const normalizeKeyPart = (value: unknown): string => {
  if (value === null || value === undefined) {
    return ''
  }
  if (typeof value === 'string') {
    return value
  }
  if (value instanceof URLSearchParams) {
    return value.toString()
  }
  if (value instanceof FormData) {
    // FormData 包含文件对象，避免序列化引发高开销或异常
    const keys: string[] = []
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    ;(value as any).forEach((_: unknown, key: string) => keys.push(key))
    return `__FORMDATA__:${keys.join(',')}`
  }
  try {
    return JSON.stringify(value)
  } catch {
    return String(value)
  }
}

const getRequestKey = (config: InternalAxiosRequestConfig) => {
  return [
    config.method,
    config.baseURL || '',
    config.url,
    normalizeKeyPart(config.params),
    normalizeKeyPart(config.data),
  ].join('&')
};

const addPendingRequest = (config: InternalAxiosRequestConfig) => {
  const key = getRequestKey(config);
  if (pendingRequests.has(key)) {
    pendingRequests.get(key)?.abort();
    pendingRequests.delete(key);
  }
  const controller = new AbortController();
  config.signal = controller.signal;
  pendingRequests.set(key, controller);
};

const removePendingRequest = (config: InternalAxiosRequestConfig) => {
  const key = getRequestKey(config);
  if (pendingRequests.has(key)) {
    pendingRequests.delete(key);
  }
};

export const cancelAllPendingRequests = () => {
  pendingRequests.forEach((controller) => {
    controller.abort();
  });
  pendingRequests.clear();
};

function startLoading() {
  if (loadingCount === 0) {
    loadingTimer = setTimeout(() => {
      loadingInstance = ElLoading.service({
        lock: true,
        text: '加载中......',
        background: 'rgba(0, 0, 0, 0.7)',
      })
    }, 300) // 300ms debounce
  }
  loadingCount++
}

function endLoading() {
  if (loadingCount <= 0) {
    return
  }
  loadingCount--
  if (loadingCount === 0) {
    if (loadingTimer) {
      clearTimeout(loadingTimer)
      loadingTimer = null
    }
    if (loadingInstance) {
      loadingInstance.close()
      loadingInstance = null
    }
  }
}

const instance = axios.create({
  baseURL: '/api',
  timeout: 20 * 1000,
  withCredentials: true,
})

interface CustomAxiosRequestConfig extends InternalAxiosRequestConfig {
  showLoading?: boolean
  errorCallback?: (errorMsg: string) => void
  showError?: boolean
  uploadProgressCallback?: (event: ProgressEvent) => void
  dataType?: string
  skipAuthRefresh?: boolean
  enableRequestDedup?: boolean
}

function subscribeTokenRefresh(callback: (token: string) => void) {
  refreshSubscribers.push(callback)
}

function onTokenRefreshed(token: string) {
  refreshSubscribers.forEach(callback => callback(token))
  refreshSubscribers = []
}

async function refreshToken(): Promise<string | null> {
  const userInfoStore = useUserInfoStore()
  const refreshToken = userInfoStore.userInfo?.refreshToken

  if (!refreshToken) {
    return null
  }

  try {
    const response = await axios.post('/api/refreshToken', new URLSearchParams({ refreshToken }).toString(), {
      headers: {
        'Content-Type': contentTypeForm,
        'X-Tenant-Id': 'default',
      },
      withCredentials: true,
    })

    if (response.data && response.data.code === 200 && response.data.data) {
      const newToken = response.data.data.token
      const newRefreshToken = response.data.data.refreshToken

      if (userInfoStore.userInfo) {
        userInfoStore.setUserInfo({
          ...userInfoStore.userInfo,
          token: newToken,
          refreshToken: newRefreshToken || refreshToken,
        })
      }

      return newToken
    }
  } catch (error) {
    if (import.meta.env.DEV) {
      const msg = (error as any)?.message ? String((error as any).message) : 'Token refresh failed'
      console.warn('[auth] Token refresh failed:', msg)
    }
  }

  return null
}

instance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const customConfig = config as CustomAxiosRequestConfig
    config.headers['X-Tenant-Id'] = 'default'

    const userInfoStore = useUserInfoStore()
    const token = userInfoStore.getToken()
    if (token && !customConfig.skipAuthRefresh) {
      config.headers.Authorization = `Bearer ${token}`
    }

    if (customConfig.enableRequestDedup !== false) {
      addPendingRequest(config)
    }

    if (customConfig.showLoading) {
      startLoading()
    }
    return config
  },
  (error: any) => {
    endLoading()
    Message.error('请求发送失败')
    return Promise.reject('请求发送失败')
  }
)

instance.interceptors.response.use(
  (response: AxiosResponse) => {
    const config = response.config as CustomAxiosRequestConfig
    const { showLoading, errorCallback, showError = true, responseType } = config
    if (showLoading) {
      endLoading()
    }
    if (config.enableRequestDedup !== false) {
      removePendingRequest(config)
    }
    const responseData = response.data
    if (responseType == 'arraybuffer' || responseType == 'blob') {
      return responseData
    }

    if (responseData.code == 200) {
      return responseData
    } else if (responseData.code == 901) {
      const customConfig = config as CustomAxiosRequestConfig

      if (customConfig.skipAuthRefresh) {
        const userInfoStore = useUserInfoStore()
        userInfoStore.clearUserInfo()
        router.push({
          path: '/login',
          query: {
            redirectUrl: router.currentRoute.value.fullPath,
          },
        })
        return Promise.reject({ showError: false, msg: '登录超时' })
      }

      if (!isRefreshing) {
        isRefreshing = true

        return refreshToken()
          .then(newToken => {
            isRefreshing = false

            if (newToken) {
              onTokenRefreshed(newToken)
              config.headers.Authorization = `Bearer ${newToken}`
              return instance(config)
            } else {
              const userInfoStore = useUserInfoStore()
              userInfoStore.clearUserInfo()
              router.push({
                path: '/login',
                query: {
                  redirectUrl: router.currentRoute.value.fullPath,
                },
              })
              return Promise.reject({ showError: false, msg: '登录超时' })
            }
          })
          .catch(error => {
            isRefreshing = false
            const userInfoStore = useUserInfoStore()
            userInfoStore.clearUserInfo()
            router.push({
              path: '/login',
              query: {
                redirectUrl: router.currentRoute.value.fullPath,
              },
            })
            return Promise.reject({ showError: false, msg: '登录超时' })
          })
      } else {
        return new Promise(resolve => {
          subscribeTokenRefresh((token: string) => {
            config.headers.Authorization = `Bearer ${token}`
            resolve(instance(config))
          })
        })
      }
    } else {
      if (errorCallback) {
        errorCallback(responseData.info)
      }
      return Promise.reject({ showError: showError, msg: responseData.info })
    }
  },
  (error: any) => {
    if (axios.isCancel(error) || error?.code === 'ERR_CANCELED') {
      return Promise.reject({ showError: false, msg: '请求已取消' })
    }
    if (error.config && error.config.showLoading) {
      endLoading()
    }
    if (error.config && error.config.enableRequestDedup !== false) {
      removePendingRequest(error.config)
    }
    return Promise.reject({ showError: true, msg: '网络异常' })
  }
)

const request = (config: {
  url: string
  params?: any
  dataType?: string
  showLoading?: boolean
  responseType?: any
  errorCallback?: (errorMsg: string) => void
  showError?: boolean
  uploadProgressCallback?: (event: ProgressEvent) => void
  enableRequestDedup?: boolean
}) => {
  const { url, params, dataType, showLoading = true, responseType = responseTypeJson } = config
  let contentType = contentTypeForm
  let requestData: any
  const headers: any = {
    'X-Requested-with': 'XMLHttpRequest',
  }

  if (dataType != null && dataType == 'json') {
    contentType = contentTypeJson
    requestData = params
    headers['Content-Type'] = contentType
  } else if (dataType === 'file') {
    const formData = new FormData()
    for (const key in params) {
      formData.append(key, params[key] == undefined ? '' : params[key])
    }
    requestData = formData
  } else {
    const urlSearchParams = new URLSearchParams()
    for (const key in params) {
      urlSearchParams.append(key, params[key] == undefined ? '' : params[key])
    }
    requestData = urlSearchParams.toString()
    headers['Content-Type'] = contentType
  }

  const axiosConfig: CustomAxiosRequestConfig = {
    headers: headers as any,
    responseType: responseType,
    showLoading: showLoading,
    errorCallback: config.errorCallback,
    showError: config.showError,
    onUploadProgress: (event: any) => {
      if (config.uploadProgressCallback) {
        config.uploadProgressCallback(event)
      }
    },
    enableRequestDedup: config.enableRequestDedup,
  } as any

  return instance.post(url, requestData, axiosConfig).catch(error => {
    if (import.meta.env.DEV) {
      const status = (error as any)?.response?.status
      const message = (error as any)?.message ? String((error as any).message) : 'Request error'
      console.error('[Request]', { url, status, message })
    }
    if (error?.msg === '请求已取消') {
      return null
    }
    if (error && error.showError) {
      Message.error(error.msg)
    }
    return null
  })
}

export default request

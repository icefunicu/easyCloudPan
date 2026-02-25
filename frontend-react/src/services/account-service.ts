import { request, requestSuccess } from '@/lib/api-client'
import type { LoginResult, ResponseVO, SessionWebUserDto, UserSpaceDto } from '@/types'

export interface SendEmailCodeParams {
  email: string
  checkCode: string
  type: number
}

export interface RegisterParams {
  email: string
  nickName: string
  password: string
  checkCode: string
  emailCode: string
}

export interface LoginParams {
  email: string
  password: string
  checkCode: string
}

export interface ResetPwdParams {
  email: string
  password: string
  checkCode: string
  emailCode: string
}

interface RequestFeedbackOptions {
  onError?: (msg: string) => void
  showError?: boolean
}

export const getCheckCodeUrl = (type = 0): string => `/api/checkCode?type=${type}&time=${Date.now()}`

export const sendEmailCode = async (params: SendEmailCodeParams, options?: RequestFeedbackOptions): Promise<boolean> => {
  return requestSuccess({
    path: '/sendEmailCode',
    params,
    showError: options?.showError ?? true,
    errorCallback: options?.onError,
  })
}

export const register = async (params: RegisterParams, options?: RequestFeedbackOptions): Promise<boolean> => {
  return requestSuccess({
    path: '/register',
    params,
    showError: options?.showError ?? true,
    errorCallback: options?.onError,
  })
}

export const login = async (params: LoginParams, options?: RequestFeedbackOptions): Promise<LoginResult | null> => {
  const data = await request<{ userInfo: SessionWebUserDto; token: string; refreshToken: string }>({
    path: '/login',
    params,
    requireSignature: false,
    showError: options?.showError ?? true,
    errorCallback: options?.onError,
  })
  if (!data) {
    return null
  }
  return {
    userInfo: data.userInfo,
    token: data.token,
    refreshToken: data.refreshToken,
  }
}

export const resetPwd = async (params: ResetPwdParams, options?: RequestFeedbackOptions): Promise<boolean> => {
  return requestSuccess({
    path: '/resetPwd',
    params,
    showError: options?.showError ?? true,
    errorCallback: options?.onError,
  })
}

export const logout = async (): Promise<boolean> => {
  return requestSuccess({ path: '/logout', params: {} })
}

export const getUseSpace = async (): Promise<UserSpaceDto | null> => {
  return request<UserSpaceDto>({ path: '/getUseSpace', params: {}, showError: false })
}

export const updatePassword = async (password: string): Promise<boolean> => {
  return requestSuccess({ path: '/updatePassword', params: { password } })
}

export const updateNickName = async (nickName: string): Promise<boolean> => {
  return requestSuccess({ path: '/updateNickName', params: { nickName } })
}

export const updateUserAvatar = async (avatar: File): Promise<boolean> => {
  return requestSuccess({
    path: '/updateUserAvatar',
    params: { avatar },
    dataType: 'file',
  })
}

export const qqLogin = async (callbackUrl?: string): Promise<string | null> => {
  return request<string>({
    path: '/qqlogin',
    params: callbackUrl ? { callbackUrl } : {},
    showError: false,
  })
}

export const qqLoginCallback = async (
  params: Record<string, string | string[] | undefined>
): Promise<{ userInfo: SessionWebUserDto; redirectUrl: string; token: string; refreshToken: string } | null> => {
  const normalizedParams: Record<string, string> = {}
  Object.entries(params).forEach(([key, value]) => {
    if (Array.isArray(value)) {
      normalizedParams[key] = value[0] || ''
    } else {
      normalizedParams[key] = value || ''
    }
  })

  const data = await request<{
    userInfo: SessionWebUserDto
    callbackUrl?: string
    token: string
    refreshToken: string
  }>({
    path: '/qqlogin/callback',
    params: normalizedParams,
    showError: false,
  })

  if (!data) {
    return null
  }

  return {
    userInfo: data.userInfo,
    redirectUrl: data.callbackUrl || '/',
    token: data.token,
    refreshToken: data.refreshToken,
  }
}

export const refreshToken = async (refreshTokenValue: string): Promise<{ token: string; refreshToken: string } | null> => {
  return request<{ token: string; refreshToken: string }>({
    path: '/refreshToken',
    params: { refreshToken: refreshTokenValue },
    showError: false,
    requireSignature: false,
  })
}

export const getAvatarUrl = (userId: string): string => `/api/getAvatar/${userId}`

export const getUserInfo = async (options?: { skipAuthRefresh?: boolean }): Promise<SessionWebUserDto | null> => {
  return request<SessionWebUserDto>({
    path: '/getUserInfo',
    params: {},
    showError: false,
    skipAuthRefresh: options?.skipAuthRefresh,
  })
}

export type AccountRawResponse<T> = ResponseVO<T>


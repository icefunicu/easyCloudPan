import request from '@/utils/Request'
import type { ResponseVO } from '@/types'
import { adaptLoginResult, adaptSessionWebUser, adaptUserSpace } from '@/adapters'
import type { SessionWebUserDto, UserSpaceDto, LoginResult } from '@/types'

const api = {
  checkCode: '/api/checkCode',
  sendEmailCode: '/sendEmailCode',
  register: '/register',
  login: '/login',
  resetPwd: '/resetPwd',
  qqlogin: '/qqlogin',
  qqloginCallback: '/qqlogin/callback',
  logout: '/logout',
  getUseSpace: '/getUseSpace',
  getAvatar: '/getAvatar',
  updatePassword: '/updatePassword',
  updateUserAvatar: '/updateUserAvatar',
  refreshToken: '/refreshToken',
  updateNickName: '/updateNickName',
}

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
  rememberMe?: boolean
}

export interface ResetPwdParams {
  email: string
  password: string
  checkCode: string
  emailCode: string
}

export interface QQLoginCallbackParams {
  [key: string]: string
}

export async function sendEmailCode(params: SendEmailCodeParams): Promise<ResponseVO<null> | null> {
  return request({ url: api.sendEmailCode, params }) as Promise<ResponseVO<null> | null>
}

export async function register(params: RegisterParams): Promise<ResponseVO<null> | null> {
  return request({ url: api.register, params }) as Promise<ResponseVO<null> | null>
}

export async function login(params: LoginParams): Promise<LoginResult | null> {
  const result = (await request({ url: api.login, params })) as ResponseVO<unknown> | null
  if (result && result.code === 200) {
    return adaptLoginResult(result.data)
  }
  return null
}

export async function resetPwd(params: ResetPwdParams): Promise<ResponseVO<null> | null> {
  return request({ url: api.resetPwd, params }) as Promise<ResponseVO<null> | null>
}

export async function qqLogin(callbackUrl?: string): Promise<string | null> {
  const result = (await request({
    url: api.qqlogin,
    params: { callbackUrl },
  })) as ResponseVO<string> | null
  return result?.data ?? null
}

export async function qqLoginCallback(
  params: QQLoginCallbackParams
): Promise<{ userInfo: SessionWebUserDto; redirectUrl: string; token: string; refreshToken: string } | null> {
  const result = (await request({
    url: api.qqloginCallback,
    params,
    showLoading: false,
  })) as ResponseVO<{ userInfo: unknown; callbackUrl?: unknown; token?: string; refreshToken?: string }> | null
  if (result && result.code === 200) {
    const redirectUrlRaw = result.data.callbackUrl
    const redirectUrl = typeof redirectUrlRaw === 'string' && redirectUrlRaw ? redirectUrlRaw : '/'
    return {
      userInfo: adaptSessionWebUser(result.data.userInfo),
      redirectUrl,
      token: (result.data.token as string) || '',
      refreshToken: (result.data.refreshToken as string) || '',
    }
  }
  return null
}

export async function logout(): Promise<ResponseVO<null> | null> {
  return request({ url: api.logout }) as Promise<ResponseVO<null> | null>
}

export async function getUseSpace(): Promise<UserSpaceDto | null> {
  const result = (await request({
    url: api.getUseSpace,
    showLoading: false,
  })) as ResponseVO<unknown> | null
  if (result && result.code === 200) {
    return adaptUserSpace(result.data)
  }
  return null
}

export async function updatePassword(password: string): Promise<ResponseVO<null> | null> {
  return request({ url: api.updatePassword, params: { password } }) as Promise<ResponseVO<null> | null>
}

export async function updateNickName(nickName: string): Promise<ResponseVO<null> | null> {
  return request({ url: api.updateNickName, params: { nickName } }) as Promise<ResponseVO<null> | null>
}

export async function updateUserAvatar(avatar: File): Promise<ResponseVO<null> | null> {
  return request({
    url: api.updateUserAvatar,
    params: { avatar },
    dataType: 'file',
  }) as Promise<ResponseVO<null> | null>
}

export function getAvatarUrl(userId: string): string {
  return `/api${api.getAvatar}/${userId}`
}

export async function refreshTokenRequest(
  refreshToken: string
): Promise<{ token: string; refreshToken: string } | null> {
  const result = (await request({
    url: api.refreshToken,
    params: { refreshToken },
    showError: false,
  })) as ResponseVO<{ token: string; refreshToken: string }> | null
  return result?.data ?? null
}

import request from '@/utils/Request'
import type { ResponseVO, SessionWebUserDto } from '@/types'

const api = {
  login: '/oauth/login',
  callback: '/oauth/callback',
  register: '/oauth/register',
}

export type OAuthCallbackData =
  | {
      status: 'login_success'
      token: string
      refreshToken: string
      userInfo: SessionWebUserDto
      callbackUrl: string
    }
  | {
      status: 'need_register'
      registerKey: string
      email: string
      nickname?: string
      avatarUrl?: string
      provider: string
    }

export async function oauthLogin(provider: string, callbackUrl?: string): Promise<string | null> {
  const result = (await request({
    url: `${api.login}/${provider}`,
    params: { callbackUrl },
    showLoading: false,
  })) as ResponseVO<string> | null

  return result?.data ?? null
}

export async function oauthCallback(
  provider: string,
  params: Record<string, unknown>
): Promise<OAuthCallbackData | null> {
  const result = (await request({
    url: `${api.callback}/${provider}`,
    params,
    showLoading: false,
  })) as ResponseVO<unknown> | null

  if (result && result.code === 200) {
    return result.data as OAuthCallbackData
  }

  return null
}

export async function oauthRegister(
  registerKey: string,
  password: string
): Promise<{ token: string; refreshToken: string; userInfo: SessionWebUserDto } | null> {
  const result = (await request({
    url: api.register,
    params: { registerKey, password },
    showLoading: false,
  })) as ResponseVO<unknown> | null

  if (result && result.code === 200) {
    const data = result.data as Record<string, unknown>
    return {
      token: data.token as string,
      refreshToken: data.refreshToken as string,
      userInfo: data.userInfo as SessionWebUserDto,
    }
  }

  return null
}

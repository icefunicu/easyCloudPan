import { request } from '@/lib/api-client'
import type { SessionWebUserDto } from '@/types'

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

export const oauthLogin = async (provider: string, callbackUrl?: string): Promise<string | null> => {
  return request<string>({
    path: `/oauth/login/${provider}`,
    params: callbackUrl ? { callbackUrl } : {},
    showError: false,
  })
}

export const oauthCallback = async (
  provider: string,
  params: Record<string, string | string[] | undefined>
): Promise<OAuthCallbackData | null> => {
  const normalizedParams: Record<string, string> = {}
  Object.entries(params).forEach(([key, value]) => {
    if (Array.isArray(value)) {
      normalizedParams[key] = value[0] || ''
    } else {
      normalizedParams[key] = value || ''
    }
  })

  return request<OAuthCallbackData>({
    path: `/oauth/callback/${provider}`,
    params: normalizedParams,
    showError: false,
  })
}

export const oauthRegister = async (
  registerKey: string,
  password: string
): Promise<{ token: string; refreshToken: string; userInfo: SessionWebUserDto } | null> => {
  return request<{ token: string; refreshToken: string; userInfo: SessionWebUserDto }>({
    path: '/oauth/register',
    params: { registerKey, password },
    showError: false,
  })
}


import type { SessionWebUserDto, UserSpaceDto, LoginResult, UserInfoVO } from '@/types'

export function adaptSessionWebUser(data: unknown): SessionWebUserDto {
  const raw = data as Record<string, unknown>
  return {
    nickName: raw.nickName as string,
    userId: raw.userId as string,
    admin: raw.admin as boolean,
    avatar: raw.avatar as string,
  }
}

export function adaptUserSpace(data: unknown): UserSpaceDto {
  const raw = data as Record<string, unknown>
  return {
    useSpace: raw.useSpace as number,
    totalSpace: raw.totalSpace as number,
  }
}

export function adaptLoginResult(data: unknown): LoginResult {
  const raw = data as Record<string, unknown>
  return {
    userInfo: adaptSessionWebUser(raw.userInfo),
    token: raw.token as string,
    refreshToken: raw.refreshToken as string,
  }
}

export function adaptUserInfo(data: unknown): UserInfoVO {
  const raw = data as Record<string, unknown>
  return {
    userId: raw.userId as string,
    nickName: raw.nickName as string,
    email: raw.email as string,
    qqAvatar: raw.qqAvatar as string,
    qqOpenId: raw.qqOpenId as string | undefined,
    joinTime: raw.joinTime as string,
    lastLoginTime: raw.lastLoginTime as string,
    status: raw.status as number,
    useSpace: raw.useSpace as number,
    totalSpace: raw.totalSpace as number,
    admin: raw.admin as boolean,
  }
}

export function adaptUserInfoList(data: unknown): UserInfoVO[] {
  const list = data as unknown[]
  return list.map(adaptUserInfo)
}

export interface SessionWebUserDto {
  userId: string
  nickName: string
  admin: boolean
  avatar: string
  tenantId?: string
  token?: string
  refreshToken?: string
}

export interface UserSpaceDto {
  useSpace: number
  totalSpace: number
}

export interface LoginResult {
  userInfo: SessionWebUserDto
  token: string
  refreshToken: string
}

export interface UserInfoVO {
  userId: string
  nickName: string
  email: string
  qqAvatar: string
  qqOpenId?: string
  joinTime: string
  lastLoginTime: string
  status: number
  useSpace: number
  totalSpace: number
  admin: boolean
}


export interface SessionWebUserDto {
  nickName: string
  userId: string
  admin: boolean
  avatar: string
}

export interface UserSpaceDto {
  useSpace: number
  totalSpace: number
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

export interface LoginResult {
  userInfo: SessionWebUserDto
  token: string
  refreshToken: string
}

export interface UpdateUserSpaceParams {
  userId: string
  changeSpace: number
}

export interface UpdateUserStatusParams {
  userId: string
  status: number
}

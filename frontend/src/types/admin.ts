import type { FileInfoVO } from './file'

export interface SysSettingsDto {
  registerEmailTitle: string
  registerEmailContent: string
  userInitUseSpace: number
}

export interface AdminFileInfo extends FileInfoVO {
  userId: string
  nickName: string
}

export interface UserInfoQuery {
  pageNo?: number
  pageSize?: number
  nickNameFuzzy?: string
  status?: number
}

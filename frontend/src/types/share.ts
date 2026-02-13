export type ShareValidType = 0 | 1 | 2 | 3

export interface FileShare {
  shareId: string
  fileId: string
  userId: string
  validType: ShareValidType
  expireTime: string | null
  shareTime: string
  code: string
  showCount: number
  fileName: string
  folderType: number
  fileCategory: number
}

export interface ShareInfoVO {
  shareTime: string
  expireTime: string | null
  nickName: string
  fileName: string
  currentUser: boolean
  fileId: string
  avatar: string
  userId: string
}

export interface ShareFileParams {
  fileId: string
  validType: ShareValidType
  code?: string
}

export interface SaveShareParams {
  shareId: string
  shareFileIds: string
  myFolderId: string
}

export const SHARE_VALID_TYPE = {
  ONE_DAY: 0 as ShareValidType,
  SEVEN_DAYS: 1 as ShareValidType,
  THIRTY_DAYS: 2 as ShareValidType,
  FOREVER: 3 as ShareValidType,
}

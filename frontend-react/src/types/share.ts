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
  status?: number
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

export interface ShareUrlVO {
  shareUrl: string
  code: string
  shareId: string
}

export interface ShareStatusVO {
  shareId: string
  fileName: string
  shareTime: string
  expireTime: string | null
  showCount: number
  validType: ShareValidType
  status: 'valid' | 'expired' | string
}


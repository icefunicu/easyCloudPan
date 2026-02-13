import type { FileShare, ShareInfoVO, PaginationResultVO } from '@/types'
import { adaptPaginationResult } from './fileAdapter'

export function adaptFileShare(data: unknown): FileShare {
  const raw = data as Record<string, unknown>
  return {
    shareId: raw.shareId as string,
    fileId: raw.fileId as string,
    userId: raw.userId as string,
    validType: raw.validType as FileShare['validType'],
    expireTime: raw.expireTime as string | null,
    shareTime: raw.shareTime as string,
    code: raw.code as string,
    showCount: raw.showCount as number,
    fileName: raw.fileName as string,
    folderType: raw.folderType as number,
    fileCategory: raw.fileCategory as number,
  }
}

export function adaptFileShareList(data: unknown): FileShare[] {
  const list = data as unknown[]
  return list.map(adaptFileShare)
}

export function adaptShareInfo(data: unknown): ShareInfoVO {
  const raw = data as Record<string, unknown>
  return {
    shareTime: raw.shareTime as string,
    expireTime: raw.expireTime as string | null,
    nickName: raw.nickName as string,
    fileName: raw.fileName as string,
    currentUser: raw.currentUser as boolean,
    fileId: raw.fileId as string,
    avatar: raw.avatar as string,
    userId: raw.userId as string,
  }
}

export function adaptFileSharePagination(data: unknown): PaginationResultVO<FileShare> {
  return adaptPaginationResult(data, adaptFileShare)
}

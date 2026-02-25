import { request, requestSuccess } from '@/lib/api-client'
import type {
  FileInfoVO,
  FileShare,
  FolderVO,
  PaginationResultVO,
  ShareInfoVO,
  ShareStatusVO,
  ShareUrlVO,
  ShareValidType,
} from '@/types'

export const loadShareList = async (params: { pageNo?: number; pageSize?: number; fileNameFuzzy?: string }): Promise<PaginationResultVO<FileShare> | null> => {
  return request<PaginationResultVO<FileShare>>({
    path: '/share/loadShareList',
    params,
    requestKey: 'share.loadShareList',
    cancelPrevious: true,
  })
}

export const cancelShare = async (shareIds: string): Promise<boolean> => {
  return requestSuccess({ path: '/share/cancelShare', params: { shareIds } })
}

export const getShareUrl = async (shareId: string): Promise<ShareUrlVO | null> => {
  return request<ShareUrlVO>({
    path: '/share/getShareUrl',
    params: { shareId },
    showError: false,
  })
}

export const checkShareStatus = async (shareId: string): Promise<ShareStatusVO | null> => {
  return request<ShareStatusVO>({
    path: '/share/checkShareStatus',
    params: { shareId },
    showError: false,
  })
}

export const shareFile = async (params: {
  fileId: string
  validType: ShareValidType
  code?: string
}): Promise<FileShare | null> => {
  return request<FileShare>({ path: '/share/shareFile', params })
}

export const getShareLoginInfo = async (shareId: string): Promise<ShareInfoVO | null> => {
  return request<ShareInfoVO | null>({ path: '/showShare/getShareLoginInfo', params: { shareId }, showError: false })
}

export const loadShareFileList = async (params: {
  pageNo?: number
  pageSize?: number
  shareId: string
  filePid?: string
}): Promise<PaginationResultVO<FileInfoVO> | null> => {
  return request<PaginationResultVO<FileInfoVO>>({
    path: '/showShare/loadFileList',
    params,
    requestKey: `share.loadShareFileList:${params.shareId}`,
    cancelPrevious: true,
  })
}

export const getShareInfo = async (shareId: string): Promise<ShareInfoVO | null> => {
  return request<ShareInfoVO>({ path: '/showShare/getShareInfo', params: { shareId }, showError: false })
}

export const checkShareCode = async (shareId: string, code: string): Promise<boolean> => {
  return requestSuccess({ path: '/showShare/checkShareCode', params: { shareId, code } })
}

export const createShareDownloadUrl = async (shareId: string, fileId: string): Promise<string | null> => {
  return request<string>({ path: `/showShare/createDownloadUrl/${shareId}/${fileId}`, params: {} })
}

export const getShareDownloadBaseUrl = (): string => '/api/showShare/download'

export const saveShare = async (params: { shareId: string; shareFileIds: string; myFolderId: string }): Promise<boolean> => {
  return requestSuccess({ path: '/showShare/saveShare', params })
}

export const getShareFolderInfo = async (shareId: string, path: string): Promise<FolderVO[] | null> => {
  return request<FolderVO[]>({ path: '/showShare/getFolderInfo', params: { shareId, path }, showError: false })
}

export const getShareImageUrl = (shareId: string, imageFolder: string, imageName: string): string =>
  `/api/showShare/getImage/${shareId}/${imageFolder}/${imageName}`

export const getShareFileUrl = (shareId: string, fileId: string): string => `/api/showShare/getFile/${shareId}/${fileId}`

export const getShareVideoUrl = (shareId: string, fileId: string): string =>
  `/api/showShare/ts/getVideoInfo/${shareId}/${fileId}`


import request from '@/utils/Request'
import type { ResponseVO, PaginationResultVO } from '@/types'
import { adaptFileSharePagination, adaptShareInfo } from '@/adapters'
import type { FileShare, ShareInfoVO, ShareFileParams, SaveShareParams } from '@/types'
import { adaptFileInfoPagination } from '@/adapters'

const api = {
  loadShareList: '/share/loadShareList',
  cancelShare: '/share/cancelShare',
  shareFile: '/share/shareFile',
  getShareLoginInfo: '/showShare/getShareLoginInfo',
  loadFileList: '/showShare/loadFileList',
  getShareInfo: '/showShare/getShareInfo',
  checkShareCode: '/showShare/checkShareCode',
  createDownloadUrl: '/showShare/createDownloadUrl',
  download: '/api/showShare/download',
  saveShare: '/showShare/saveShare',
  getFolderInfo: '/showShare/getFolderInfo',
  getImage: '/api/showShare/getImage',
  getFile: '/showShare/getFile',
  getVideoInfo: '/showShare/ts/getVideoInfo',
}

export interface LoadShareListParams {
  pageNo?: number
  pageSize?: number
}

export interface CancelShareParams {
  shareIds: string
}

export interface LoadFileListParams {
  pageNo?: number
  pageSize?: number
  shareId: string
  filePid?: string
}

export interface CheckShareCodeParams {
  shareId: string
  code: string
}

export async function loadShareList(params: LoadShareListParams): Promise<PaginationResultVO<FileShare> | null> {
  const result = await request({ url: api.loadShareList, params }) as ResponseVO<unknown> | null
  if (result && result.code === 200) {
    return adaptFileSharePagination(result.data)
  }
  return null
}

export async function cancelShare(shareIds: string): Promise<ResponseVO<null> | null> {
  return request({ url: api.cancelShare, params: { shareIds } }) as Promise<ResponseVO<null> | null>
}

export async function shareFile(params: ShareFileParams): Promise<FileShare | null> {
  const result = await request({ url: api.shareFile, params }) as ResponseVO<unknown> | null
  if (result && result.code === 200) {
    const raw = result.data as Record<string, unknown>
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
  return null
}

export async function getShareLoginInfo(shareId: string): Promise<ShareInfoVO | null> {
  const result = await request({ 
    url: api.getShareLoginInfo, 
    params: { shareId },
    showLoading: false,
  }) as ResponseVO<unknown> | null
  if (result && result.code === 200) {
    if (result.data === null) return null
    return adaptShareInfo(result.data)
  }
  return null
}

export async function loadFileList(params: LoadFileListParams): Promise<PaginationResultVO<import('@/types').FileInfoVO> | null> {
  const result = await request({ url: api.loadFileList, params }) as ResponseVO<unknown> | null
  if (result && result.code === 200) {
    return adaptFileInfoPagination(result.data)
  }
  return null
}

export async function getShareInfo(shareId: string): Promise<ShareInfoVO | null> {
  const result = await request({ 
    url: api.getShareInfo, 
    params: { shareId },
  }) as ResponseVO<unknown> | null
  if (result && result.code === 200) {
    return adaptShareInfo(result.data)
  }
  return null
}

export async function checkShareCode(params: CheckShareCodeParams): Promise<ResponseVO<null> | null> {
  return request({ url: api.checkShareCode, params }) as Promise<ResponseVO<null> | null>
}

export async function createDownloadUrl(shareId: string, fileId: string): Promise<string | null> {
  const result = await request({ 
    url: `${api.createDownloadUrl}/${shareId}/${fileId}`,
  }) as ResponseVO<string> | null
  return result?.data ?? null
}

export function getDownloadUrl(code: string): string {
  return `${api.download}/${code}`
}

export async function saveShare(params: SaveShareParams): Promise<ResponseVO<null> | null> {
  return request({ url: api.saveShare, params }) as Promise<ResponseVO<null> | null>
}

export async function getFolderInfo(shareId: string, path: string): Promise<import('@/types').FolderVO[] | null> {
  const result = await request({ 
    url: api.getFolderInfo, 
    params: { shareId, path },
    showLoading: false,
  }) as ResponseVO<unknown[]> | null
  if (result && result.code === 200) {
    const { adaptFolderList } = await import('@/adapters')
    return adaptFolderList(result.data)
  }
  return null
}

export function getImageUrl(shareId: string, imageFolder: string, imageName: string): string {
  return `${api.getImage}/${shareId}/${imageFolder}/${imageName}`
}

export function getFileUrl(shareId: string, fileId: string): string {
  return `/api${api.getFile}/${shareId}/${fileId}`
}

export function getVideoUrl(shareId: string, fileId: string): string {
  return `/api${api.getVideoInfo}/${shareId}/${fileId}`
}

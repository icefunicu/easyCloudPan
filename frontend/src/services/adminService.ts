import request from '@/utils/Request'
import type { ResponseVO, PaginationResultVO } from '@/types'
import { adaptSysSettings, adaptUserInfoList, adaptFileInfoPagination } from '@/adapters'
import type { SysSettingsDto, UserInfoVO, UserInfoQuery, FileInfoVO } from '@/types'
import { adaptFolderList } from '@/adapters'

const api = {
  loadUserList: '/admin/loadUserList',
  updateUserStatus: '/admin/updateUserStatus',
  updateUserSpace: '/admin/updateUserSpace',
  getSysSettings: '/admin/getSysSettings',
  saveSysSettings: '/admin/saveSysSettings',
  loadFileList: '/admin/loadFileList',
  delFile: '/admin/delFile',
  createDownloadUrl: '/admin/createDownloadUrl',
  download: '/api/admin/download',
  getFolderInfo: '/admin/getFolderInfo',
  getImage: '/api/admin/getImage',
  getFile: '/admin/getFile',
  getVideoInfo: '/admin/ts/getVideoInfo',
}

export interface UpdateUserStatusParams {
  userId: string
  status: number
}

export interface UpdateUserSpaceParams {
  userId: string
  changeSpace: number
}

export interface SaveSysSettingsParams {
  registerEmailTitle: string
  registerEmailContent: string
  userInitUseSpace: number
}

export interface LoadFileListParams {
  pageNo?: number
  pageSize?: number
  fileNameFuzzy?: string
  filePid?: string
}

export interface DelFileParams {
  fileIdAndUserIds: string
}

export async function loadUserList(params: UserInfoQuery): Promise<PaginationResultVO<UserInfoVO> | null> {
  const result = (await request({ url: api.loadUserList, params })) as ResponseVO<unknown> | null
  if (result && result.code === 200) {
    const raw = result.data as Record<string, unknown>
    return {
      list: adaptUserInfoList(raw.list),
      pageNo: raw.pageNo as number,
      pageSize: raw.pageSize as number,
      totalCount: raw.totalCount as number,
      pageTotal: raw.pageTotal as number,
    }
  }
  return null
}

export async function updateUserStatus(params: UpdateUserStatusParams): Promise<ResponseVO<null> | null> {
  return request({ url: api.updateUserStatus, params }) as Promise<ResponseVO<null> | null>
}

export async function updateUserSpace(params: UpdateUserSpaceParams): Promise<ResponseVO<null> | null> {
  return request({ url: api.updateUserSpace, params }) as Promise<ResponseVO<null> | null>
}

export async function getSysSettings(): Promise<SysSettingsDto | null> {
  const result = (await request({ url: api.getSysSettings })) as ResponseVO<unknown> | null
  if (result && result.code === 200) {
    return adaptSysSettings(result.data)
  }
  return null
}

export async function saveSysSettings(params: SaveSysSettingsParams): Promise<ResponseVO<null> | null> {
  return request({ url: api.saveSysSettings, params }) as Promise<ResponseVO<null> | null>
}

export async function loadFileList(
  params: LoadFileListParams,
  showLoading: boolean = true
): Promise<PaginationResultVO<FileInfoVO> | null> {
  const result = (await request({
    url: api.loadFileList,
    params,
    showLoading: showLoading,
  })) as ResponseVO<unknown> | null
  if (result && result.code === 200) {
    return adaptFileInfoPagination(result.data)
  }
  return null
}

export async function delFile(fileIdAndUserIds: string): Promise<ResponseVO<null> | null> {
  return request({ url: api.delFile, params: { fileIdAndUserIds } }) as Promise<ResponseVO<null> | null>
}

export async function createDownloadUrl(userId: string, fileId: string): Promise<string | null> {
  const result = (await request({
    url: `${api.createDownloadUrl}/${userId}/${fileId}`,
  })) as ResponseVO<string> | null
  return result?.data ?? null
}

export function getDownloadUrl(code: string): string {
  return `${api.download}/${code}`
}

export async function getFolderInfo(path: string): Promise<import('@/types').FolderVO[] | null> {
  const result = (await request({
    url: api.getFolderInfo,
    params: { path },
    showLoading: false,
  })) as ResponseVO<unknown[]> | null
  if (result && result.code === 200) {
    return adaptFolderList(result.data)
  }
  return null
}

export function getImageUrl(userId: string, imageFolder: string, imageName: string): string {
  return `${api.getImage}/${userId}/${imageFolder}/${imageName}`
}

export function getFileUrl(userId: string, fileId: string): string {
  return `/api${api.getFile}/${userId}/${fileId}`
}

export function getVideoUrl(userId: string, fileId: string): string {
  return `/api${api.getVideoInfo}/${userId}/${fileId}`
}

import { request, requestSuccess } from '@/lib/api-client'
import type { FileInfoVO, FolderVO, PaginationResultVO, SysSettingsDto, UserInfoVO } from '@/types'

export const loadUserList = async (params: Record<string, unknown>): Promise<PaginationResultVO<UserInfoVO> | null> => {
  return request<PaginationResultVO<UserInfoVO>>({
    path: '/admin/loadUserList',
    params,
    requestKey: 'admin.loadUserList',
    cancelPrevious: true,
  })
}

export const updateUserStatus = async (userId: string, status: number): Promise<boolean> => {
  return requestSuccess({ path: '/admin/updateUserStatus', params: { userId, status } })
}

export const updateUserSpace = async (userId: string, changeSpace: number): Promise<boolean> => {
  return requestSuccess({ path: '/admin/updateUserSpace', params: { userId, changeSpace } })
}

export const setUserSpace = async (userId: string, totalSpaceMB: number): Promise<boolean> => {
  return requestSuccess({ path: '/admin/setUserSpace', params: { userId, totalSpaceMB } })
}

export const getSysSettings = async (): Promise<SysSettingsDto | null> => {
  return request<SysSettingsDto>({ path: '/admin/getSysSettings', params: {} })
}

export const saveSysSettings = async (params: SysSettingsDto): Promise<boolean> => {
  return requestSuccess({ path: '/admin/saveSysSettings', params })
}

export const loadAdminFileList = async (params: {
  pageNo?: number
  pageSize?: number
  fileNameFuzzy?: string
  filePid?: string
}): Promise<PaginationResultVO<FileInfoVO> | null> => {
  return request<PaginationResultVO<FileInfoVO>>({
    path: '/admin/loadFileList',
    params,
    requestKey: 'admin.loadFileList',
    cancelPrevious: true,
  })
}

export const deleteAdminFile = async (fileIdAndUserIds: string): Promise<boolean> => {
  return requestSuccess({ path: '/admin/delFile', params: { fileIdAndUserIds } })
}

export const createAdminDownloadUrl = async (userId: string, fileId: string): Promise<string | null> => {
  return request<string>({ path: `/admin/createDownloadUrl/${userId}/${fileId}`, params: {} })
}

export const getAdminDownloadBaseUrl = (): string => '/api/admin/download'

export const getAdminFolderInfo = async (path: string): Promise<FolderVO[] | null> => {
  return request<FolderVO[]>({
    path: '/admin/getFolderInfo',
    params: { path },
    showError: false,
    requestKey: 'admin.getFolderInfo',
    cancelPrevious: true,
  })
}

export const getAdminImageUrl = (userId: string, imageFolder: string, imageName: string): string =>
  `/api/admin/getImage/${userId}/${imageFolder}/${imageName}`

export const getAdminFileUrl = (userId: string, fileId: string): string => `/api/admin/getFile/${userId}/${fileId}`

export const getAdminVideoUrl = (userId: string, fileId: string): string => `/api/admin/ts/getVideoInfo/${userId}/${fileId}`


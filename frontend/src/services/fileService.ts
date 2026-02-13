import request from '@/utils/Request'
import type { ResponseVO, PaginationResultVO } from '@/types'
import { 
  adaptFileInfo, 
  adaptFileInfoList, 
  adaptFolderList, 
  adaptUploadResult, 
  adaptFileInfoPagination,
  adaptUploadedChunks 
} from '@/adapters'
import type { FileInfoVO, FolderVO, UploadResultDto, FileInfoQuery } from '@/types'

const api = {
  loadDataList: '/file/loadDataList',
  rename: '/file/rename',
  newFoloder: '/file/newFoloder',
  getFolderInfo: '/file/getFolderInfo',
  delFile: '/file/delFile',
  changeFileFolder: '/file/changeFileFolder',
  createDownloadUrl: '/file/createDownloadUrl',
  download: '/api/file/download',
  uploadFile: '/file/uploadFile',
  uploadedChunks: '/file/uploadedChunks',
  loadAllFolder: '/file/loadAllFolder',
  getImage: '/api/file/getImage',
  getFile: '/file/getFile',
  getVideoInfo: '/file/ts/getVideoInfo',
}

export interface LoadDataListParams extends FileInfoQuery {
  category?: string
}

export interface RenameParams {
  fileId: string
  fileName: string
}

export interface NewFolderParams {
  filePid: string
  fileName: string
}

export interface GetFolderInfoParams {
  path: string
}

export interface DelFileParams {
  fileIds: string
}

export interface ChangeFileFolderParams {
  fileIds: string
  filePid: string
}

export interface UploadFileParams {
  file: File
  fileName: string
  fileMd5: string
  chunkIndex: number
  chunks: number
  fileId: string
  filePid: string
}

export interface UploadedChunksParams {
  fileId: string
  fileMd5?: string
}

export interface LoadAllFolderParams {
  filePid: string
  currentFileIds?: string
}

export async function loadDataList(params: LoadDataListParams): Promise<PaginationResultVO<FileInfoVO> | null> {
  const result = await request({ 
    url: api.loadDataList, 
    params,
    showLoading: true,
  }) as ResponseVO<unknown> | null
  if (result && result.code === 200) {
    return adaptFileInfoPagination(result.data)
  }
  return null
}

export async function rename(params: RenameParams): Promise<FileInfoVO | null> {
  const result = await request({ url: api.rename, params }) as ResponseVO<unknown> | null
  if (result && result.code === 200) {
    return adaptFileInfo(result.data)
  }
  return null
}

export async function newFolder(params: NewFolderParams): Promise<FileInfoVO | null> {
  const result = await request({ url: api.newFoloder, params }) as ResponseVO<unknown> | null
  if (result && result.code === 200) {
    return adaptFileInfo(result.data)
  }
  return null
}

export async function getFolderInfo(path: string): Promise<FolderVO[] | null> {
  const result = await request({ 
    url: api.getFolderInfo, 
    params: { path },
    showLoading: false,
  }) as ResponseVO<unknown[]> | null
  if (result && result.code === 200) {
    return adaptFolderList(result.data)
  }
  return null
}

export async function delFile(fileIds: string): Promise<ResponseVO<null> | null> {
  return request({ url: api.delFile, params: { fileIds } }) as Promise<ResponseVO<null> | null>
}

export async function changeFileFolder(params: ChangeFileFolderParams): Promise<ResponseVO<null> | null> {
  return request({ url: api.changeFileFolder, params }) as Promise<ResponseVO<null> | null>
}

export async function createDownloadUrl(fileId: string): Promise<string | null> {
  const result = await request({ 
    url: `${api.createDownloadUrl}/${fileId}`,
  }) as ResponseVO<string> | null
  return result?.data ?? null
}

export function getDownloadUrl(code: string): string {
  return `${api.download}/${code}`
}

export async function uploadFile(
  params: UploadFileParams,
  onProgress?: (event: ProgressEvent) => void
): Promise<UploadResultDto | null> {
  const result = await request({ 
    url: api.uploadFile,
    params,
    dataType: 'file',
    showLoading: false,
    showError: false,
    uploadProgressCallback: onProgress,
  }) as ResponseVO<unknown> | null
  if (result && result.code === 200) {
    return adaptUploadResult(result.data)
  }
  return null
}

export async function getUploadedChunks(params: UploadedChunksParams): Promise<number[] | null> {
  const result = await request({ 
    url: api.uploadedChunks,
    params,
    showLoading: false,
  }) as ResponseVO<unknown> | null
  if (result && result.code === 200) {
    return adaptUploadedChunks(result.data)
  }
  return null
}

export async function loadAllFolder(params: LoadAllFolderParams): Promise<FileInfoVO[] | null> {
  const result = await request({ url: api.loadAllFolder, params }) as ResponseVO<unknown[]> | null
  if (result && result.code === 200) {
    return adaptFileInfoList(result.data)
  }
  return null
}

export function getImageUrl(imageFolder: string, imageName: string): string {
  return `${api.getImage}/${imageFolder}/${imageName}`
}

export function getFileUrl(fileId: string): string {
  return `/api${api.getFile}/${fileId}`
}

export function getVideoUrl(fileId: string): string {
  return `/api${api.getVideoInfo}/${fileId}`
}

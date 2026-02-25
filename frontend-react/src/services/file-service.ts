import { request, requestSuccess, type AxiosProgressEvent } from '@/lib/api-client'
import type { FileInfoQuery, FileInfoVO, FolderVO, PaginationResultVO, UploadResultDto } from '@/types'

export interface UploadFileParams {
  file: Blob
  fileName: string
  fileMd5: string
  chunkIndex: number
  chunks: number
  fileId: string
  filePid: string
}

export interface LoadDataListParams extends FileInfoQuery {
  category?: string
}

export const loadDataList = async (params: LoadDataListParams): Promise<PaginationResultVO<FileInfoVO> | null> => {
  return request<PaginationResultVO<FileInfoVO>>({
    path: '/file/loadDataList',
    params,
    requestKey: 'file.loadDataList',
    cancelPrevious: true,
  })
}

export const renameFile = async (fileId: string, fileName: string): Promise<FileInfoVO | null> => {
  return request<FileInfoVO>({ path: '/file/rename', params: { fileId, fileName } })
}

export const newFolder = async (filePid: string, fileName: string): Promise<FileInfoVO | null> => {
  return request<FileInfoVO>({ path: '/file/newFoloder', params: { filePid, fileName } })
}

export const getFolderInfo = async (path: string): Promise<FolderVO[] | null> => {
  return request<FolderVO[]>({
    path: '/file/getFolderInfo',
    params: { path },
    showError: false,
    requestKey: 'file.getFolderInfo',
    cancelPrevious: true,
  })
}

export const deleteFile = async (fileIds: string): Promise<boolean> => {
  return requestSuccess({ path: '/file/delFile', params: { fileIds } })
}

export const changeFileFolder = async (fileIds: string, filePid: string): Promise<boolean> => {
  return requestSuccess({ path: '/file/changeFileFolder', params: { fileIds, filePid } })
}

export const createDownloadUrl = async (fileId: string): Promise<string | null> => {
  return request<string>({ path: `/file/createDownloadUrl/${fileId}`, params: {} })
}

export const getDownloadBaseUrl = (): string => '/api/file/download'

export const batchDownloadFiles = async (fileIds: string): Promise<Blob | null> => {
  return request<Blob>({
    path: `/file/batchDownload/${encodeURIComponent(fileIds)}`,
    params: {},
    responseType: 'blob',
    showError: false,
  })
}

export const uploadFileChunk = async (
  params: UploadFileParams,
  onProgress?: (event: AxiosProgressEvent) => void,
  errorCallback?: (msg: string) => void
): Promise<UploadResultDto | null> => {
  return request<UploadResultDto>({
    path: '/file/uploadFile',
    params,
    dataType: 'file',
    showError: false,
    uploadProgressCallback: onProgress,
    errorCallback,
  })
}

export const getUploadedChunks = async (fileId: string, filePid: string): Promise<number[] | null> => {
  return request<number[]>({
    path: '/file/uploadedChunks',
    params: { fileId, filePid },
    showError: false,
  })
}

export const getTransferStatus = async (fileId: string): Promise<number | null> => {
  const data = await request<{ status: number }>({
    path: '/file/transferStatus',
    params: { fileId },
    showError: false,
  })
  return data?.status ?? null
}

export const loadAllFolder = async (filePid: string, currentFileIds?: string): Promise<FileInfoVO[] | null> => {
  return request<FileInfoVO[]>({
    path: '/file/loadAllFolder',
    params: { filePid, currentFileIds: currentFileIds || '' },
  })
}

export const getImageUrl = (imageFolder: string, imageName: string): string =>
  `/api/file/getImage/${imageFolder}/${imageName}`

export const getFileUrl = (fileId: string): string => `/api/file/getFile/${fileId}`

export const getVideoUrl = (fileId: string): string => `/api/file/ts/getVideoInfo/${fileId}`

export const getFileInfoById = async (fileId: string): Promise<FileInfoVO | null> => {
  return request<FileInfoVO>({ path: `/file/getFileInfo/${fileId}`, params: {}, showError: false })
}

export const loadDataListCursor = async (cursor?: string, pageSize = 30) => {
  return request<{
    list: FileInfoVO[]
    nextCursor: string | null
    pageSize: number
    hasMore: boolean
  }>({
    path: '/file/loadDataListCursor',
    params: { cursor: cursor || '', pageSize },
    showError: false,
  })
}


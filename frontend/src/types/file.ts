export type FileFolderType = 0 | 1
export type FileCategory = 1 | 2 | 3 | 4 | 5
export type FileType = 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10
export type FileStatus = 0 | 1 | 2

export interface FileInfoVO {
  fileId: string
  filePid: string
  fileSize: number
  fileName: string
  fileCover: string
  recoveryTime?: string
  lastUpdateTime: string
  folderType: FileFolderType
  fileCategory: FileCategory
  fileType: FileType
  status: FileStatus
  userId?: string
  nickName?: string
}

export interface FolderVO {
  fileId: string
  fileName: string
  filePid: string
}

export interface UploadResultDto {
  fileId: string
  status: 'uploading' | 'upload_finish' | 'upload_seconds' | 'fail' | 'init' | 'emptyfile' | 'retrying' | 'network_error' | 'auth_error' | 'server_error'
}

export interface FileInfoQuery {
  pageNo?: number
  pageSize?: number
  fileNameFuzzy?: string
  filePid?: string
  category?: string
  userId?: string
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

export interface CreateDownloadUrlResult {
  code: string
}

export const FILE_FOLDER_TYPE = {
  FILE: 0 as FileFolderType,
  FOLDER: 1 as FileFolderType,
}

export const FILE_CATEGORY = {
  VIDEO: 1 as FileCategory,
  AUDIO: 2 as FileCategory,
  IMAGE: 3 as FileCategory,
  DOC: 4 as FileCategory,
  OTHER: 5 as FileCategory,
}

export const FILE_TYPE = {
  VIDEO: 1 as FileType,
  AUDIO: 2 as FileType,
  IMAGE: 3 as FileType,
  PDF: 4 as FileType,
  DOC: 5 as FileType,
  EXCEL: 6 as FileType,
  TXT: 7 as FileType,
  CODE: 8 as FileType,
  ZIP: 9 as FileType,
  OTHER: 10 as FileType,
}

export const FILE_STATUS = {
  TRANSCODING: 0 as FileStatus,
  FAILED: 1 as FileStatus,
  SUCCESS: 2 as FileStatus,
}

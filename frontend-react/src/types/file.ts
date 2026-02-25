export type FileFolderType = 0 | 1
export type FileCategory = 1 | 2 | 3 | 4 | 5
export type FileType = 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10
export type FileStatus = 0 | 1 | 2

export interface FileInfoVO {
  fileId: string
  filePid: string
  fileName: string
  fileSize: number
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
  status:
    | 'uploading'
    | 'upload_finish'
    | 'upload_seconds'
    | 'fail'
    | 'init'
    | 'emptyfile'
    | 'retrying'
    | 'network_error'
    | 'auth_error'
    | 'server_error'
}

export interface FileInfoQuery {
  pageNo?: number
  pageSize?: number
  fileNameFuzzy?: string
  filePid?: string
  category?: string
  userId?: string
}


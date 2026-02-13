import type { FileInfoVO, FolderVO, UploadResultDto, PaginationResultVO } from '@/types'

export function adaptFileInfo(data: unknown): FileInfoVO {
  const raw = data as Record<string, unknown>
  return {
    fileId: raw.fileId as string,
    filePid: raw.filePid as string,
    fileSize: raw.fileSize as number,
    fileName: raw.fileName as string,
    fileCover: raw.fileCover as string,
    recoveryTime: raw.recoveryTime as string | undefined,
    lastUpdateTime: raw.lastUpdateTime as string,
    folderType: raw.folderType as FileInfoVO['folderType'],
    fileCategory: raw.fileCategory as FileInfoVO['fileCategory'],
    fileType: raw.fileType as FileInfoVO['fileType'],
    status: raw.status as FileInfoVO['status'],
    userId: raw.userId as string | undefined,
    nickName: raw.nickName as string | undefined,
  }
}

export function adaptFileInfoList(data: unknown): FileInfoVO[] {
  const list = data as unknown[]
  return list.map(adaptFileInfo)
}

export function adaptFolder(data: unknown): FolderVO {
  const raw = data as Record<string, unknown>
  return {
    fileId: raw.fileId as string,
    fileName: raw.fileName as string,
    filePid: raw.filePid as string,
  }
}

export function adaptFolderList(data: unknown): FolderVO[] {
  const list = data as unknown[]
  return list.map(adaptFolder)
}

export function adaptUploadResult(data: unknown): UploadResultDto {
  const raw = data as Record<string, unknown>
  return {
    fileId: raw.fileId as string,
    status: raw.status as UploadResultDto['status'],
  }
}

export function adaptPaginationResult<T>(data: unknown, itemAdapter: (item: unknown) => T): PaginationResultVO<T> {
  const raw = data as Record<string, unknown>
  return {
    list: (raw.list as unknown[]).map(itemAdapter),
    pageNo: raw.pageNo as number,
    pageSize: raw.pageSize as number,
    totalCount: raw.totalCount as number,
    pageTotal: raw.pageTotal as number,
  }
}

export function adaptFileInfoPagination(data: unknown): PaginationResultVO<FileInfoVO> {
  return adaptPaginationResult(data, adaptFileInfo)
}

export function adaptUploadedChunks(data: unknown): number[] {
  return data as number[]
}

import { request, requestSuccess } from '@/lib/api-client'
import type { FileInfoVO, PaginationResultVO } from '@/types'

export const loadRecycleList = async (params: { pageNo?: number; pageSize?: number }): Promise<PaginationResultVO<FileInfoVO> | null> => {
  return request<PaginationResultVO<FileInfoVO>>({
    path: '/recycle/loadRecycleList',
    params,
    requestKey: 'recycle.loadRecycleList',
    cancelPrevious: true,
  })
}

export const recoverFile = async (fileIds: string): Promise<boolean> => {
  return requestSuccess({ path: '/recycle/recoverFile', params: { fileIds } })
}

export const deleteRecycleFile = async (fileIds: string): Promise<boolean> => {
  return requestSuccess({ path: '/recycle/delFile', params: { fileIds } })
}


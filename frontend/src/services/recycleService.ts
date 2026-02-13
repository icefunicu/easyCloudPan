import request from '@/utils/Request'
import type { ResponseVO, PaginationResultVO } from '@/types'
import { adaptFileInfoPagination } from '@/adapters'
import type { FileInfoVO } from '@/types'

const api = {
  loadRecycleList: '/recycle/loadRecycleList',
  delFile: '/recycle/delFile',
  recoverFile: '/recycle/recoverFile',
}

export interface LoadRecycleListParams {
  pageNo?: number
  pageSize?: number
}

export async function loadRecycleList(params: LoadRecycleListParams): Promise<PaginationResultVO<FileInfoVO> | null> {
  const result = await request({ url: api.loadRecycleList, params }) as ResponseVO<unknown> | null
  if (result && result.code === 200) {
    return adaptFileInfoPagination(result.data)
  }
  return null
}

export async function delFile(fileIds: string): Promise<ResponseVO<null> | null> {
  return request({ url: api.delFile, params: { fileIds } }) as Promise<ResponseVO<null> | null>
}

export async function recoverFile(fileIds: string): Promise<ResponseVO<null> | null> {
  return request({ url: api.recoverFile, params: { fileIds } }) as Promise<ResponseVO<null> | null>
}

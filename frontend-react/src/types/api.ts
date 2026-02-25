export interface ResponseVO<T = unknown> {
  status: 'success' | 'error' | string
  code: number
  info: string
  suggestion?: string
  data: T
}

export interface PaginationResultVO<T> {
  list: T[]
  pageNo: number
  pageSize: number
  totalCount: number
  pageTotal: number
}

export interface CursorPage<T> {
  list: T[]
  nextCursor: string | null
  hasMore: boolean
  pageSize: number
  totalCount?: number
}


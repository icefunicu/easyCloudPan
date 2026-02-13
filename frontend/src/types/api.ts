export interface ResponseVO<T = unknown> {
  status: string
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
  pageSize: number
  hasMore: boolean
}

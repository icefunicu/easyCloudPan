import request from '@/utils/Request'
import type { ResponseVO } from '@/types'
import { toRequestPath } from '@/utils/url'

export async function createDownloadCode(createDownloadUrl: string): Promise<string | null> {
  const requestUrl = toRequestPath(createDownloadUrl)
  const result = (await request({ url: requestUrl })) as ResponseVO<string> | null
  return result?.data ?? null
}

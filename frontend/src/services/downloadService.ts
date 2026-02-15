import request from '@/utils/Request'
import type { ResponseVO } from '@/types'

export async function createDownloadCode(createDownloadUrl: string): Promise<string | null> {
  const result = (await request({ url: createDownloadUrl })) as ResponseVO<string> | null
  return result?.data ?? null
}

import request from '@/utils/Request'
import { toRequestPath } from '@/utils/url'

export async function fetchBlob(url: string): Promise<Blob | null> {
  const requestUrl = toRequestPath(url)
  return (await request({
    url: requestUrl,
    responseType: 'blob',
    enableRequestDedup: false,
  })) as Blob | null
}

export async function fetchArrayBuffer(url: string): Promise<ArrayBuffer | null> {
  const requestUrl = toRequestPath(url)
  return (await request({
    url: requestUrl,
    responseType: 'arraybuffer',
    enableRequestDedup: false,
  })) as ArrayBuffer | null
}

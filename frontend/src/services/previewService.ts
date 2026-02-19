import request from '@/utils/Request'

export async function fetchBlob(url: string): Promise<Blob | null> {
  return (await request({
    url,
    responseType: 'blob',
    enableRequestDedup: false,
  })) as Blob | null
}

export async function fetchArrayBuffer(url: string): Promise<ArrayBuffer | null> {
  return (await request({
    url,
    responseType: 'arraybuffer',
    enableRequestDedup: false,
  })) as ArrayBuffer | null
}

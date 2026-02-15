import request from '@/utils/Request'

export async function fetchBlob(url: string): Promise<Blob | null> {
  return (await request({
    url,
    responseType: 'blob',
  })) as Blob | null
}

export async function fetchArrayBuffer(url: string): Promise<ArrayBuffer | null> {
  return (await request({
    url,
    responseType: 'arraybuffer',
  })) as ArrayBuffer | null
}

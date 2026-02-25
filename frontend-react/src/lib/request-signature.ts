const SIGNATURE_SECRET = import.meta.env.VITE_SIGNATURE_SECRET || 'easypan-default-secret'
const SIGNATURE_VERSION = 'v2'

const textEncoder = new TextEncoder()
let hmacKeyPromise: Promise<CryptoKey> | null = null

const getHmacKey = async (): Promise<CryptoKey> => {
  if (!hmacKeyPromise) {
    hmacKeyPromise = crypto.subtle.importKey(
      'raw',
      textEncoder.encode(SIGNATURE_SECRET),
      { name: 'HMAC', hash: 'SHA-256' },
      false,
      ['sign']
    )
  }
  return hmacKeyPromise
}

const bytesToHex = (input: ArrayBuffer): string => {
  const bytes = new Uint8Array(input)
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('')
}

const normalizePath = (path: string): string => {
  if (!path) {
    return '/api'
  }
  if (/^https?:\/\//i.test(path)) {
    try {
      return new URL(path).pathname
    } catch {
      return '/api'
    }
  }
  const normalized = path.startsWith('/') ? path : `/${path}`
  if (normalized.startsWith('/api/')) {
    return normalized
  }
  return `/api${normalized}`
}

const randomNonce = (): string => {
  const t = Date.now().toString(36)
  const r = Math.random().toString(36).slice(2, 12)
  return `${t}-${r}`
}

export interface RequestSignHeaders {
  'X-Timestamp': string
  'X-Nonce': string
  'X-Signature': string
  'X-Signature-Version': string
}

export const signRequest = async (method: string, path: string, body = ''): Promise<RequestSignHeaders> => {
  const timestamp = Date.now().toString()
  const nonce = randomNonce()
  const payload = [timestamp, nonce, method.toUpperCase(), normalizePath(path), body].join('&')
  const key = await getHmacKey()
  const signature = bytesToHex(await crypto.subtle.sign('HMAC', key, textEncoder.encode(payload)))

  return {
    'X-Timestamp': timestamp,
    'X-Nonce': nonce,
    'X-Signature': signature,
    'X-Signature-Version': SIGNATURE_VERSION,
  }
}


const SIGNATURE_SECRET = import.meta.env.VITE_SIGNATURE_SECRET || 'easypan-default-secret'
const SIGNATURE_VERSION = 'v2'

export interface SignatureHeaders {
  'X-Timestamp': string
  'X-Nonce': string
  'X-Signature': string
  'X-Signature-Version': string
}

const textEncoder = new TextEncoder()
let hmacKeyPromise: Promise<CryptoKey> | null = null

const getHmacKey = (): Promise<CryptoKey> => {
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

const bytesToHex = (buffer: ArrayBuffer): string => {
  const bytes = new Uint8Array(buffer)
  return Array.from(bytes, byte => byte.toString(16).padStart(2, '0')).join('')
}

const normalizePath = (path: string): string => {
  if (!path) {
    return '/api'
  }
  if (/^https?:\/\//i.test(path)) {
    try {
      const parsed = new URL(path)
      return parsed.pathname
    } catch {
      return '/api'
    }
  }

  const withLeadingSlash = path.startsWith('/') ? path : `/${path}`
  if (withLeadingSlash.startsWith('/api/')) {
    return withLeadingSlash
  }
  return `/api${withLeadingSlash}`
}

const generateNonce = (): string => {
  const timestampPart = Date.now().toString(36)
  const randomPart = Math.random().toString(36).slice(2, 12)
  return `${timestampPart}-${randomPart}`
}

const generateSignature = async (
  timestamp: string,
  nonce: string,
  method: string,
  path: string,
  body = ''
): Promise<string> => {
  const canonicalPath = normalizePath(path)
  const canonicalMethod = method.toUpperCase()
  const payload = [timestamp, nonce, canonicalMethod, canonicalPath, body].join('&')
  const key = await getHmacKey()
  const signed = await crypto.subtle.sign('HMAC', key, textEncoder.encode(payload))
  return bytesToHex(signed)
}

export const signRequest = async (
  method: string,
  path: string,
  body = ''
): Promise<SignatureHeaders> => {
  const timestamp = Date.now().toString()
  const nonce = generateNonce()
  const signature = await generateSignature(timestamp, nonce, method, path, body)

  return {
    'X-Timestamp': timestamp,
    'X-Nonce': nonce,
    'X-Signature': signature,
    'X-Signature-Version': SIGNATURE_VERSION,
  }
}

export const RequestSignatureConfig = {
  SIGNATURE_VERSION,
}


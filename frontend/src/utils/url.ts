const HTTP_URL_PATTERN = /^https?:\/\//i

export const isHttpUrl = (value: string): boolean => {
  return HTTP_URL_PATTERN.test(value)
}

const ensureLeadingSlash = (value: string): string => {
  return value.startsWith('/') ? value : `/${value}`
}

export const toApiPath = (value: string): string => {
  if (!value) {
    return '/api'
  }
  if (isHttpUrl(value)) {
    return value
  }
  const normalized = ensureLeadingSlash(value)
  if (normalized === '/api' || normalized.startsWith('/api/')) {
    return normalized
  }
  return `/api${normalized}`
}

export const toRequestPath = (value: string): string => {
  if (!value) {
    return '/'
  }
  if (isHttpUrl(value)) {
    return value
  }
  const normalized = ensureLeadingSlash(value)
  if (normalized === '/api') {
    return '/'
  }
  if (normalized.startsWith('/api/')) {
    return normalized.slice(4)
  }
  return normalized
}

export const resolveDownloadTarget = (
  downloadBaseUrl: string | null | undefined,
  codeOrUrl: string | null | undefined
): string | null => {
  if (!codeOrUrl) {
    return null
  }
  if (isHttpUrl(codeOrUrl)) {
    return codeOrUrl
  }
  const base = (downloadBaseUrl || '').replace(/\/+$/, '')
  if (!base) {
    return codeOrUrl
  }
  return `${base}/${codeOrUrl}`
}

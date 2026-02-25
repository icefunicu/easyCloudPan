export const isHttpUrl = (value: string): boolean => /^https?:\/\//i.test(value)

export const resolveDownloadTarget = (base: string, codeOrUrl: string): string => {
  if (isHttpUrl(codeOrUrl)) {
    return codeOrUrl
  }
  const normalizedBase = base.replace(/\/+$/, '')
  return `${normalizedBase}/${codeOrUrl}`
}

export const downloadBlob = (blob: Blob, fileName: string): void => {
  const objectUrl = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(objectUrl)
}


export const sizeToText = (value?: number | null): string => {
  const parsed = Number(value ?? 0)
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return '0B'
  }
  const units = ['B', 'KB', 'MB', 'GB', 'TB', 'PB']
  let current = parsed
  let index = 0
  while (current >= 1024 && index < units.length - 1) {
    current /= 1024
    index += 1
  }
  const precision = index === 0 ? 0 : current >= 100 ? 0 : current >= 10 ? 1 : 2
  return `${Number(current.toFixed(precision))}${units[index]}`
}

export const percent = (used: number, total: number): number => {
  if (!total || total <= 0) {
    return 0
  }
  return Math.max(0, Math.min(100, Math.round((used / total) * 100)))
}

export const safeText = (input: unknown): string => {
  if (typeof input !== 'string') {
    return ''
  }
  return input.trim()
}


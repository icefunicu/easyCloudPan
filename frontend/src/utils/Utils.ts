export default {
  size2Str: (limit: number) => {
    const parsed = Number(limit)
    if (!Number.isFinite(parsed) || parsed <= 0) {
      return '0B'
    }

    const units = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']
    let value = parsed
    let unitIndex = 0

    while (value >= 1024 && unitIndex < units.length - 1) {
      value /= 1024
      unitIndex += 1
    }

    const precision = unitIndex === 0 ? 0 : value >= 100 ? 0 : value >= 10 ? 1 : 2
    const normalized = Number(value.toFixed(precision))
    return `${normalized}${units[unitIndex]}`
  },
}

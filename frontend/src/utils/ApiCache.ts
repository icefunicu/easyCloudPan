/**
 * API 响应缓存工具
 * 用于减少重复请求，提升前端性能
 */

interface CacheEntry<T> {
  data: T
  timestamp: number
  ttl: number
  etag?: string
}

interface CacheOptions {
  ttl?: number // 缓存时间（毫秒），默认 5 分钟
  staleWhileRevalidate?: boolean // 是否在后台刷新时返回过期数据
  cacheKey?: string // 自定义缓存键
}

const DEFAULT_TTL = 5 * 60 * 1000 // 5 分钟
const MAX_CACHE_SIZE = 100

class ApiCache {
  private cache = new Map<string, CacheEntry<unknown>>()
  private pendingRequests = new Map<string, Promise<unknown>>()

  /**
   * 生成缓存键
   */
  private generateKey(url: string, params?: Record<string, unknown>): string {
    const paramsStr = params ? JSON.stringify(params) : ''
    return `${url}:${paramsStr}`
  }

  /**
   * 检查缓存是否过期
   */
  private isExpired<T>(entry: CacheEntry<T>): boolean {
    return Date.now() - entry.timestamp > entry.ttl
  }

  /**
   * 清理过期缓存
   */
  private cleanup(): void {
    const now = Date.now()
    for (const [key, entry] of this.cache.entries()) {
      if (now - entry.timestamp > entry.ttl * 2) {
        this.cache.delete(key)
      }
    }

    // 如果缓存超过最大大小，删除最旧的条目
    if (this.cache.size > MAX_CACHE_SIZE) {
      const entries = [...this.cache.entries()]
        .sort((a, b) => a[1].timestamp - b[1].timestamp)
      const toDelete = entries.slice(0, this.cache.size - MAX_CACHE_SIZE)
      toDelete.forEach(([key]) => this.cache.delete(key))
    }
  }

  /**
   * 获取缓存数据
   */
  get<T>(url: string, params?: Record<string, unknown>): T | null {
    const key = this.generateKey(url, params)
    const entry = this.cache.get(key) as CacheEntry<T> | undefined

    if (!entry) {
      return null
    }

    if (this.isExpired(entry)) {
      // 过期缓存，返回 null（可配合 staleWhileRevalidate 使用）
      return null
    }

    return entry.data
  }

  /**
   * 设置缓存数据
   */
  set<T>(url: string, data: T, options?: CacheOptions): void {
    const key = options?.cacheKey || this.generateKey(url)
    const ttl = options?.ttl || DEFAULT_TTL

    this.cache.set(key, {
      data,
      timestamp: Date.now(),
      ttl,
    })

    this.cleanup()
  }

  /**
   * 包装异步请求，自动缓存
   */
  async fetch<T>(
    url: string,
    fetcher: () => Promise<T>,
    params?: Record<string, unknown>,
    options?: CacheOptions
  ): Promise<T> {
    const key = this.generateKey(url, params)

    // 检查是否有正在进行的相同请求
    const pending = this.pendingRequests.get(key)
    if (pending) {
      return pending as Promise<T>
    }

    // 检查缓存
    const cached = this.get<T>(url, params)
    if (cached !== null) {
      // 如果启用 staleWhileRevalidate，在后台刷新缓存
      if (options?.staleWhileRevalidate) {
        this.pendingRequests.set(key, fetcher())
        this.pendingRequests.get(key)?.then(data => {
          this.set(url, data, options)
        }).finally(() => {
          this.pendingRequests.delete(key)
        })
      }
      return cached
    }

    // 发起请求
    const promise = fetcher()
    this.pendingRequests.set(key, promise)

    try {
      const data = await promise
      this.set(url, data, options)
      return data
    } finally {
      this.pendingRequests.delete(key)
    }
  }

  /**
   * 使缓存失效
   */
  invalidate(url: string, params?: Record<string, unknown>): void {
    const key = this.generateKey(url, params)
    this.cache.delete(key)
  }

  /**
   * 使匹配前缀的所有缓存失效
   */
  invalidateByPrefix(prefix: string): void {
    for (const key of this.cache.keys()) {
      if (key.startsWith(prefix)) {
        this.cache.delete(key)
      }
    }
  }

  /**
   * 清空所有缓存
   */
  clear(): void {
    this.cache.clear()
    this.pendingRequests.clear()
  }

  /**
   * 获取缓存统计信息
   */
  getStats(): { size: number; hitRate: number } {
    return {
      size: this.cache.size,
      hitRate: 0, // 需要额外计数器来计算
    }
  }
}

// 单例导出
export const apiCache = new ApiCache()

// 便捷方法
export const cachedFetch = <T>(
  url: string,
  fetcher: () => Promise<T>,
  params?: Record<string, unknown>,
  options?: CacheOptions
): Promise<T> => apiCache.fetch(url, fetcher, params, options)

// 缓存键常量
export const CACHE_KEYS = {
  FILE_LIST: '/file/loadDataList',
  FILE_INFO: '/file/getFile',
  USER_INFO: '/getUserInfo',
  USER_SPACE: '/getUseSpace',
  ALL_FOLDER: '/file/loadAllFolder',
  SHARE_LIST: '/share/loadShareList',
} as const

// 预设缓存配置
export const CACHE_PRESETS = {
  SHORT: { ttl: 30 * 1000 }, // 30 秒
  MEDIUM: { ttl: 5 * 60 * 1000 }, // 5 分钟
  LONG: { ttl: 30 * 60 * 1000 }, // 30 分钟
  STALE_WHILE_REVALIDATE: { staleWhileRevalidate: true, ttl: 5 * 60 * 1000 },
} as const

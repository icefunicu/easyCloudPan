/**
 * 请求签名工具
 * 用于防止重放攻击，确保请求的完整性和唯一性
 */

import { hash as md5Hash } from 'spark-md5'

interface SignatureHeaders {
  'X-Timestamp': string
  'X-Nonce': string
  'X-Signature': string
}

// 签名密钥（应从环境变量或安全配置中获取）
const SIGNATURE_SECRET = import.meta.env.VITE_SIGNATURE_SECRET || 'easypan-default-secret'

/**
 * 生成随机 nonce
 */
const generateNonce = (): string => {
  const timestamp = Date.now().toString(36)
  const random = Math.random().toString(36).substring(2, 15)
  return `${timestamp}-${random}`
}

/**
 * 生成签名
 * @param timestamp 时间戳
 * @param nonce 随机字符串
 * @param method HTTP 方法
 * @param path 请求路径
 * @param body 请求体
 */
const generateSignature = (
  timestamp: string,
  nonce: string,
  method: string,
  path: string,
  body?: string
): string => {
  const data = [timestamp, nonce, method.toUpperCase(), path, body || ''].join('&')
  const input = data + SIGNATURE_SECRET
  return md5Hash(input)
}

/**
 * 为请求生成签名头
 * @param method HTTP 方法
 * @param path 请求路径
 * @param body 请求体（可选）
 */
export const signRequest = (
  method: string,
  path: string,
  body?: unknown
): SignatureHeaders => {
  const timestamp = Date.now().toString()
  const nonce = generateNonce()
  const bodyStr = body ? (typeof body === 'string' ? body : JSON.stringify(body)) : undefined
  const signature = generateSignature(timestamp, nonce, method, path, bodyStr)

  return {
    'X-Timestamp': timestamp,
    'X-Nonce': nonce,
    'X-Signature': signature,
  }
}

/**
 * 敏感操作验证码管理
 */
export const SensitiveOperation = {
  // 敏感操作列表
  OPERATIONS: {
    DELETE_FILE: 'delete_file',
    BATCH_DELETE: 'batch_delete',
    CHANGE_PASSWORD: 'change_password',
    RESET_PASSWORD: 'reset_password',
    DELETE_ACCOUNT: 'delete_account',
    SHARE_CREATE: 'share_create',
    SHARE_SAVE: 'share_save',
    MOVE_FILE: 'move_file',
    BATCH_MOVE: 'batch_move',
  } as const,

  // 需要二次验证的操作
  REQUIRES_VERIFICATION: new Set([
    'delete_file',
    'batch_delete',
    'change_password',
    'reset_password',
    'delete_account',
  ]),

  /**
   * 检查操作是否需要二次验证
   */
  requiresVerification(operation: string): boolean {
    return this.REQUIRES_VERIFICATION.has(operation)
  },

  /**
   * 获取操作验证码缓存键
   */
  getVerificationKey(operation: string): string {
    return `sensitive_verification_${operation}`
  },
}

/**
 * 验证码会话管理
 */
export const VerificationSession = {
  // 验证码有效期（毫秒）
  VALIDITY_MS: 5 * 60 * 1000, // 5 分钟

  /**
   * 设置验证码会话
   */
  setVerified(operation: string): void {
    const key = SensitiveOperation.getVerificationKey(operation)
    const expiry = Date.now() + VerificationSession.VALIDITY_MS
    sessionStorage.setItem(key, expiry.toString())
  },

  /**
   * 检查是否已验证
   */
  isVerified(operation: string): boolean {
    const key = SensitiveOperation.getVerificationKey(operation)
    const expiry = sessionStorage.getItem(key)
    if (!expiry) {
      return false
    }
    const expiryTime = parseInt(expiry, 10)
    if (Date.now() > expiryTime) {
      sessionStorage.removeItem(key)
      return false
    }
    return true
  },

  /**
   * 清除验证状态
   */
  clearVerified(operation: string): void {
    const key = SensitiveOperation.getVerificationKey(operation)
    sessionStorage.removeItem(key)
  },
}

export default {
  signRequest,
  SensitiveOperation,
  VerificationSession,
}

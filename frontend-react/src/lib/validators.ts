const EMAIL_REG = /^[\w-]+(\.[\w-]+)*@[\w-]+(\.[\w-]+)+$/
const PASSWORD_REG = /^(?=.*\d)(?=.*[a-zA-Z])(?=.*[~!@#$%^&*_])[\da-zA-Z~!@#$%^&*_]{8,}$/
const SHARE_CODE_REG = /^[A-Za-z0-9]{5}$/

// 常见弱密码列表，用于检测弱密码
const WEAK_PASSWORDS = [
  '12345678',
  'password',
  'qwertyui',
  '123456789',
  'password123',
  'admin123',
  'welcome1',
  'letmein123',
  'monkey123',
  'sunshine1'
];

export const isEmail = (value: string): boolean => EMAIL_REG.test(value)
export const isPassword = (value: string): boolean => PASSWORD_REG.test(value)
export const isShareCode = (value: string): boolean => SHARE_CODE_REG.test(value)

export const passwordError = (value: string): string | null => {
  if (!value) return '密码不能为空'
  if (value.length < 8) return '密码长度至少 8 位'
  if (value.length > 128) return '密码长度不能超过 128 位'
  if (!/\d/.test(value)) return '密码必须包含数字'
  if (!/[a-zA-Z]/.test(value)) return '密码必须包含字母'
  if (!/[~!@#$%^&*_\-+=<>?]/.test(value)) return '密码必须包含特殊字符（~!@#$%^&*_-=+<>?）'
  if (WEAK_PASSWORDS.some(weak => value.toLowerCase().includes(weak))) return '密码过于简单，请使用更复杂的密码'
  if (/(\w)\1{2,}/.test(value)) return '密码不能包含连续相同的字符'
  if (!PASSWORD_REG.test(value)) return '密码格式不正确'
  return null
}

// 检查密码强度等级
export const getPasswordStrength = (value: string): 'weak' | 'medium' | 'strong' => {
  if (!value) return 'weak';
  
  let score = 0;
  
  // 长度评分
  if (value.length >= 8) score++;
  if (value.length >= 12) score++;
  
  // 字符类型评分
  if (/[a-z]/.test(value)) score++; // 小写字母
  if (/[A-Z]/.test(value)) score++; // 大写字母
  if (/\d/.test(value)) score++; // 数字
  if (/[^a-zA-Z\d]/.test(value)) score++; // 特殊字符
  
  // 复杂度评分
  if (value.length >= 16) score++;
  
  if (score < 4) return 'weak';
  if (score < 6) return 'medium';
  return 'strong';
}


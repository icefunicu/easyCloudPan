const EMAIL_REG = /^[\w-]+(\.[\w-]+)*@[\w-]+(\.[\w-]+)+$/
const PASSWORD_REG = /^(?=.*\d)(?=.*[a-zA-Z])(?=.*[~!@#$%^&*_])[\da-zA-Z~!@#$%^&*_]{8,}$/
const SHARE_CODE_REG = /^[A-Za-z0-9]{5}$/

export const isEmail = (value: string): boolean => EMAIL_REG.test(value)
export const isPassword = (value: string): boolean => PASSWORD_REG.test(value)
export const isShareCode = (value: string): boolean => SHARE_CODE_REG.test(value)

export const passwordError = (value: string): string | null => {
  if (!value) return '密码不能为空'
  if (value.length < 8) return '密码长度至少 8 位'
  if (!/\d/.test(value)) return '密码必须包含数字'
  if (!/[a-zA-Z]/.test(value)) return '密码必须包含字母'
  if (!/[~!@#$%^&*_]/.test(value)) return '密码必须包含特殊字符（~!@#$%^&*_）'
  if (!PASSWORD_REG.test(value)) return '密码格式不正确'
  return null
}


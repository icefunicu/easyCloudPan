import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  App,
  Alert,
  Button,
  Checkbox,
  Divider,
  Flex,
  Form,
  Input,
  Modal,
  Progress,
  Space,
  Tabs,
  Typography,
} from 'antd'
import {
  CloudUploadOutlined,
  FolderOpenOutlined,
  LockOutlined,
  MailOutlined,
  SafetyCertificateOutlined,
  ThunderboltOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { motion } from 'framer-motion'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { getCheckCodeUrl, login, qqLogin, register, resetPwd, sendEmailCode } from '@/services/account-service'
import { oauthLogin } from '@/services/oauth-service'
import { useAuthStore } from '@/store/auth-store'
import { isEmail, passwordError } from '@/lib/validators'
import './auth.css'

const { Title, Text } = Typography

type AuthMode = 'login' | 'register' | 'reset'
const MAIL_CODE_COOLDOWN_SECONDS = 60

interface LoginForm {
  email: string
  password: string
  checkCode: string
  rememberMe?: boolean
}

interface RegisterForm {
  email: string
  nickName: string
  registerPassword: string
  confirmPassword: string
  checkCode: string
  emailCode: string
}

interface ResetForm {
  email: string
  registerPassword: string
  confirmPassword: string
  checkCode: string
  emailCode: string
}

const features = [
  {
    icon: <CloudUploadOutlined />,
    title: '大文件分片上传',
    desc: '断点续传、秒传校验与并发上传策略',
  },
  {
    icon: <ThunderboltOutlined />,
    title: '稳定鉴权链路',
    desc: 'JWT 刷新、签名请求与租户头自动注入',
  },
  {
    icon: <FolderOpenOutlined />,
    title: '完整文件能力',
    desc: '预览、分享、回收站与管理端联动',
  },
]

export const LoginPage = () => {
  const { message } = App.useApp()
  const navigate = useNavigate()
  const setUser = useAuthStore((state) => state.setUser)
  const [searchParams] = useSearchParams()

  const [mode, setMode] = useState<AuthMode>('login')
  const [captchaUrl, setCaptchaUrl] = useState(getCheckCodeUrl(0))
  const [mailCodeCaptchaUrl, setMailCodeCaptchaUrl] = useState(getCheckCodeUrl(1))
  const [mailCodeModalOpen, setMailCodeModalOpen] = useState(false)
  const [sendingMailCode, setSendingMailCode] = useState(false)
  const [submitLoading, setSubmitLoading] = useState(false)
  const [mailCodeCooldown, setMailCodeCooldown] = useState(0)
  const [authError, setAuthError] = useState('')

  const [loginForm] = Form.useForm<LoginForm>()
  const [registerForm] = Form.useForm<RegisterForm>()
  const [resetForm] = Form.useForm<ResetForm>()
  const [sendMailCodeForm] = Form.useForm<{ checkCode: string }>()

  const redirectUrl = useMemo(() => searchParams.get('redirectUrl') || '/main/all', [searchParams])
  const mailCodeButtonText = mailCodeCooldown > 0 ? `重新发送（${mailCodeCooldown}s）` : '获取验证码'

  const resolveFormByMode = useCallback(() => {
    if (mode === 'register') {
      return registerForm
    }
    if (mode === 'reset') {
      return resetForm
    }
    return loginForm
  }, [loginForm, mode, registerForm, resetForm])

  const clearAuthError = useCallback(() => {
    setAuthError('')
  }, [])

  const captureAuthError = useCallback(
    (rawMsg: string) => {
      const msg = (rawMsg || '').trim() || '操作失败，请稍后重试'
      setAuthError(msg)

      const form = resolveFormByMode() as unknown as {
        setFields: (fields: Array<{ name: string; errors: string[] }>) => void
      }
      if (msg.includes('验证码')) {
        const targetField = msg.includes('邮箱') ? 'emailCode' : 'checkCode'
        form.setFields([{ name: targetField, errors: [msg] }])
        return
      }

      if (msg.includes('邮箱')) {
        form.setFields([{ name: 'email', errors: [msg] }])
        return
      }

      if (msg.includes('密码')) {
        form.setFields([{ name: mode === 'login' ? 'password' : 'registerPassword', errors: [msg] }])
      }
    },
    [mode, resolveFormByMode]
  )

  useEffect(() => {
    if (mailCodeCooldown <= 0) {
      return
    }
    const timer = window.setInterval(() => {
      setMailCodeCooldown((prev) => (prev <= 1 ? 0 : prev - 1))
    }, 1000)
    return () => {
      window.clearInterval(timer)
    }
  }, [mailCodeCooldown])

  const refreshCaptcha = (type: 0 | 1 = 0) => {
    if (type === 0) {
      setCaptchaUrl(getCheckCodeUrl(0))
    } else {
      setMailCodeCaptchaUrl(getCheckCodeUrl(1))
    }
  }

  const currentEmail = () => {
    if (mode === 'register') {
      return registerForm.getFieldValue('email')
    }
    if (mode === 'reset') {
      return resetForm.getFieldValue('email')
    }
    return loginForm.getFieldValue('email')
  }

  const openMailCodeModal = () => {
    if (mailCodeCooldown > 0) {
      message.info(`验证码已发送，请 ${mailCodeCooldown}s 后再试`)
      return
    }

    const email = currentEmail()
    if (!email || !isEmail(email)) {
      message.warning('请先输入正确邮箱')
      return
    }

    clearAuthError()
    sendMailCodeForm.resetFields()
    refreshCaptcha(1)
    setMailCodeModalOpen(true)
  }

  const handleSendMailCode = async () => {
    if (mailCodeCooldown > 0) {
      return
    }

    const values = await sendMailCodeForm.validateFields()
    const email = currentEmail()
    if (!email) {
      return
    }

    clearAuthError()
    setSendingMailCode(true)
    const ok = await sendEmailCode(
      {
        email,
        checkCode: values.checkCode,
        type: mode === 'register' ? 0 : 1,
      },
      {
        onError: captureAuthError,
      }
    )
    setSendingMailCode(false)

    if (ok) {
      message.success('验证码已发送，请检查邮箱')
      setMailCodeCooldown(MAIL_CODE_COOLDOWN_SECONDS)
      setMailCodeModalOpen(false)
      return
    }

    refreshCaptcha(1)
  }

  const handleLogin = async () => {
    const values = await loginForm.validateFields()
    clearAuthError()
    setSubmitLoading(true)
    const data = await login(
      {
        email: values.email,
        password: values.password,
        checkCode: values.checkCode,
      },
      {
        onError: captureAuthError,
      }
    )
    setSubmitLoading(false)

    if (!data) {
      refreshCaptcha(0)
      return
    }

    if (values.rememberMe) {
      localStorage.setItem('easycloudpan_login_email', values.email)
    } else {
      localStorage.removeItem('easycloudpan_login_email')
    }

    setUser({
      ...data.userInfo,
      token: data.token,
      refreshToken: data.refreshToken,
    })

    message.success('登录成功')
    navigate(redirectUrl)
  }

  const handleRegister = async () => {
    const values = await registerForm.validateFields()
    clearAuthError()
    setSubmitLoading(true)
    const ok = await register(
      {
        email: values.email,
        nickName: values.nickName,
        password: values.registerPassword,
        checkCode: values.checkCode,
        emailCode: values.emailCode,
      },
      {
        onError: captureAuthError,
      }
    )
    setSubmitLoading(false)

    if (!ok) {
      refreshCaptcha(0)
      return
    }

    message.success('注册成功，请登录')
    setMode('login')
    refreshCaptcha(0)
    loginForm.setFieldsValue({ email: values.email })
  }

  const handleReset = async () => {
    const values = await resetForm.validateFields()
    clearAuthError()
    setSubmitLoading(true)
    const ok = await resetPwd(
      {
        email: values.email,
        password: values.registerPassword,
        checkCode: values.checkCode,
        emailCode: values.emailCode,
      },
      {
        onError: captureAuthError,
      }
    )
    setSubmitLoading(false)

    if (!ok) {
      refreshCaptcha(0)
      return
    }

    message.success('密码重置成功，请登录')
    setMode('login')
    refreshCaptcha(0)
  }

  const doOAuthLogin = async (provider: string) => {
    const url = await oauthLogin(provider, redirectUrl)
    if (url) {
      window.location.href = url
    }
  }

  const doQqLogin = async () => {
    const url = await qqLogin(redirectUrl)
    if (url) {
      window.location.href = url
    }
  }

  return (
    <div className="auth-page app-page">
      <div className="auth-bg-grid" />
      <div className="auth-bg-glow auth-bg-glow-a" />
      <div className="auth-bg-glow auth-bg-glow-b" />

      <motion.div
        className="auth-shell"
        initial={{ opacity: 0, y: 20, scale: 0.98 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
      >
        <div className="auth-brand-section">
          <span className="auth-badge">EasyCloudPan Workspace</span>
          <Title level={2}>新一代私有云盘前端</Title>
          <Text className="desc">
            基于 React + TanStack Query + Zustand 构建，完整覆盖账号、文件、分享、回收站和管理端能力。
          </Text>
          <div className="feature-grid">
            {features.map((item, index) => (
              <motion.div
                key={item.title}
                className="auth-feature-card"
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.35, delay: index * 0.08 + 0.15 }}
              >
                <span className="icon">{item.icon}</span>
                <div>
                  <h4>{item.title}</h4>
                  <p>{item.desc}</p>
                </div>
              </motion.div>
            ))}
          </div>
        </div>

        <div className="auth-form-section">
          <Tabs
            activeKey={mode}
            onChange={(key) => {
              setMode(key as AuthMode)
              clearAuthError()
              refreshCaptcha(0)
            }}
            items={[
              { key: 'login', label: '登录' },
              { key: 'register', label: '注册' },
              { key: 'reset', label: '找回密码' },
            ]}
          />

          <motion.div
            key={mode}
            initial={{ opacity: 0, x: 10 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.3 }}
          >
            {mode === 'login' ? (
              <Form<LoginForm>
                form={loginForm}
                layout="vertical"
                initialValues={{ email: localStorage.getItem('easycloudpan_login_email') || '', rememberMe: true }}
                onValuesChange={authError ? clearAuthError : undefined}
                onFinish={handleLogin}
              >
                <Form.Item
                  label="邮箱"
                  name="email"
                  rules={[
                    { required: true, message: '请输入邮箱' },
                    { validator: (_, value) => (isEmail(value) ? Promise.resolve() : Promise.reject(new Error('邮箱格式错误'))) },
                  ]}
                >
                  <Input prefix={<MailOutlined />} placeholder="请输入邮箱" />
                </Form.Item>

                <Form.Item label="密码" name="password" rules={[{ required: true, message: '请输入密码' }]}>
                  <Input.Password prefix={<LockOutlined />} placeholder="请输入密码" />
                </Form.Item>

                <Form.Item label="验证码" required>
                  <Flex gap={10}>
                    <Form.Item name="checkCode" rules={[{ required: true, message: '请输入验证码' }]} noStyle>
                      <Input prefix={<SafetyCertificateOutlined />} placeholder="图形验证码" />
                    </Form.Item>
                    <img src={captchaUrl} alt="captcha" className="captcha-image" onClick={() => refreshCaptcha(0)} />
                  </Flex>
                </Form.Item>

                <Form.Item name="rememberMe" valuePropName="checked">
                  <Checkbox>记住我</Checkbox>
                </Form.Item>

                {authError ? <Alert className="auth-inline-error" type="error" showIcon message={authError} /> : null}

                <Button type="primary" htmlType="submit" loading={submitLoading} block>
                  登录
                </Button>

                <Divider>第三方登录</Divider>
                <Space wrap className="oauth-buttons">
                  <Button onClick={doQqLogin}>QQ</Button>
                  <Button onClick={() => void doOAuthLogin('github')}>GitHub</Button>
                  <Button onClick={() => void doOAuthLogin('gitee')}>Gitee</Button>
                  <Button onClick={() => void doOAuthLogin('google')}>Google</Button>
                  <Button onClick={() => void doOAuthLogin('microsoft')}>Microsoft</Button>
                </Space>
              </Form>
            ) : null}

            {mode === 'register' ? (
              <Form<RegisterForm> form={registerForm} layout="vertical" onValuesChange={authError ? clearAuthError : undefined} onFinish={handleRegister}>
                <Form.Item
                  label="邮箱"
                  name="email"
                  rules={[
                    { required: true, message: '请输入邮箱' },
                    { validator: (_, value) => (isEmail(value) ? Promise.resolve() : Promise.reject(new Error('邮箱格式错误'))) },
                  ]}
                >
                  <Input prefix={<MailOutlined />} />
                </Form.Item>

                <Form.Item label="昵称" name="nickName" rules={[{ required: true, message: '请输入昵称' }, { max: 20, message: '昵称最多 20 个字符' }]}>
                  <Input prefix={<UserOutlined />} maxLength={20} />
                </Form.Item>

                <Form.Item label="邮箱验证码" required>
                  <Flex gap={10}>
                    <Form.Item name="emailCode" rules={[{ required: true, message: '请输入邮箱验证码' }]} noStyle>
                      <Input placeholder="请输入邮箱验证码" />
                    </Form.Item>
                    <Button disabled={mailCodeCooldown > 0} onClick={openMailCodeModal}>
                      {mailCodeButtonText}
                    </Button>
                  </Flex>
                </Form.Item>

                <Form.Item
                  label="密码"
                  name="registerPassword"
                  rules={[
                    {
                      required: true,
                      validator: (_, value) => {
                        const error = passwordError(value || '')
                        return error ? Promise.reject(new Error(error)) : Promise.resolve()
                      },
                    },
                  ]}
                >
                  <Input.Password prefix={<LockOutlined />} />
                </Form.Item>

                <Form.Item shouldUpdate noStyle>
                  {() => {
                    const pwd = registerForm.getFieldValue('registerPassword') || ''
                    let score = 0
                    if (pwd.length >= 8) score += 25
                    if (/[a-z]/.test(pwd)) score += 25
                    if (/[A-Z]/.test(pwd)) score += 25
                    if (/\d/.test(pwd)) score += 25
                    return pwd ? <Progress percent={score} size="small" showInfo={false} /> : null
                  }}
                </Form.Item>

                <Form.Item
                  label="确认密码"
                  name="confirmPassword"
                  dependencies={['registerPassword']}
                  rules={[
                    { required: true, message: '请确认密码' },
                    ({ getFieldValue }) => ({
                      validator(_, value) {
                        if (!value || getFieldValue('registerPassword') === value) {
                          return Promise.resolve()
                        }
                        return Promise.reject(new Error('两次输入密码不一致'))
                      },
                    }),
                  ]}
                >
                  <Input.Password prefix={<LockOutlined />} />
                </Form.Item>

                <Form.Item label="图形验证码" required>
                  <Flex gap={10}>
                    <Form.Item name="checkCode" rules={[{ required: true, message: '请输入图形验证码' }]} noStyle>
                      <Input prefix={<SafetyCertificateOutlined />} placeholder="图形验证码" />
                    </Form.Item>
                    <img src={captchaUrl} alt="captcha" className="captcha-image" onClick={() => refreshCaptcha(0)} />
                  </Flex>
                </Form.Item>

                {authError ? <Alert className="auth-inline-error" type="error" showIcon message={authError} /> : null}

                <Button type="primary" htmlType="submit" loading={submitLoading} block>
                  注册
                </Button>
              </Form>
            ) : null}

            {mode === 'reset' ? (
              <Form<ResetForm> form={resetForm} layout="vertical" onValuesChange={authError ? clearAuthError : undefined} onFinish={handleReset}>
                <Form.Item
                  label="邮箱"
                  name="email"
                  rules={[
                    { required: true, message: '请输入邮箱' },
                    { validator: (_, value) => (isEmail(value) ? Promise.resolve() : Promise.reject(new Error('邮箱格式错误'))) },
                  ]}
                >
                  <Input prefix={<MailOutlined />} />
                </Form.Item>

                <Form.Item label="邮箱验证码" required>
                  <Flex gap={10}>
                    <Form.Item name="emailCode" rules={[{ required: true, message: '请输入邮箱验证码' }]} noStyle>
                      <Input placeholder="请输入邮箱验证码" />
                    </Form.Item>
                    <Button disabled={mailCodeCooldown > 0} onClick={openMailCodeModal}>
                      {mailCodeButtonText}
                    </Button>
                  </Flex>
                </Form.Item>

                <Form.Item
                  label="新密码"
                  name="registerPassword"
                  rules={[
                    {
                      required: true,
                      validator: (_, value) => {
                        const error = passwordError(value || '')
                        return error ? Promise.reject(new Error(error)) : Promise.resolve()
                      },
                    },
                  ]}
                >
                  <Input.Password prefix={<LockOutlined />} />
                </Form.Item>

                <Form.Item
                  label="确认密码"
                  name="confirmPassword"
                  dependencies={['registerPassword']}
                  rules={[
                    { required: true, message: '请确认密码' },
                    ({ getFieldValue }) => ({
                      validator(_, value) {
                        if (!value || getFieldValue('registerPassword') === value) {
                          return Promise.resolve()
                        }
                        return Promise.reject(new Error('两次输入密码不一致'))
                      },
                    }),
                  ]}
                >
                  <Input.Password prefix={<LockOutlined />} />
                </Form.Item>

                <Form.Item label="图形验证码" required>
                  <Flex gap={10}>
                    <Form.Item name="checkCode" rules={[{ required: true, message: '请输入图形验证码' }]} noStyle>
                      <Input prefix={<SafetyCertificateOutlined />} placeholder="图形验证码" />
                    </Form.Item>
                    <img src={captchaUrl} alt="captcha" className="captcha-image" onClick={() => refreshCaptcha(0)} />
                  </Flex>
                </Form.Item>

                {authError ? <Alert className="auth-inline-error" type="error" showIcon message={authError} /> : null}

                <Button type="primary" htmlType="submit" loading={submitLoading} block>
                  重置密码
                </Button>
              </Form>
            ) : null}
          </motion.div>
        </div>
      </motion.div>

      <Modal
        title="发送邮箱验证码"
        open={mailCodeModalOpen}
        confirmLoading={sendingMailCode}
        okButtonProps={{ disabled: mailCodeCooldown > 0 }}
        onCancel={() => setMailCodeModalOpen(false)}
        onOk={handleSendMailCode}
      >
        <Form form={sendMailCodeForm} layout="vertical">
          <Form.Item label="目标邮箱">
            <Text strong>{currentEmail()}</Text>
          </Form.Item>
          <Form.Item label="图形验证码" name="checkCode" rules={[{ required: true, message: '请输入图形验证码' }]}>
            <Flex gap={10}>
              <Input placeholder="图形验证码" />
              <img src={mailCodeCaptchaUrl} alt="captcha" className="captcha-image" onClick={() => refreshCaptcha(1)} />
            </Flex>
          </Form.Item>
        </Form>
      </Modal>
    </div >
  )
}

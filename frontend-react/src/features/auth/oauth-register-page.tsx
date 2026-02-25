import { useEffect, useMemo } from 'react'
import { App, Button, Card, Form, Input, Typography } from 'antd'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { oauthRegister } from '@/services/oauth-service'
import { useAuthStore } from '@/store/auth-store'
import { passwordError } from '@/lib/validators'

const { Title, Text } = Typography

interface FormValue {
  password: string
  confirmPassword: string
}

export const OAuthRegisterPage = () => {
  const { message } = App.useApp()
  const navigate = useNavigate()
  const [search] = useSearchParams()
  const [form] = Form.useForm<FormValue>()
  const setUser = useAuthStore((state) => state.setUser)

  const registerKey = search.get('registerKey') || ''
  const email = search.get('email') || ''
  const nickname = search.get('nickname') || '新用户'
  const provider = search.get('provider') || 'OAuth'

  const providerLabel = useMemo(() => {
    const map: Record<string, string> = {
      github: 'GitHub',
      gitee: 'Gitee',
      google: 'Google',
      microsoft: 'Microsoft',
    }
    return map[provider] || provider
  }, [provider])

  useEffect(() => {
    if (!registerKey) {
      navigate('/login', { replace: true })
    }
  }, [navigate, registerKey])

  if (!registerKey) {
    return null
  }

  return (
    <div className="auth-page app-page" style={{ display: 'grid', placeItems: 'center' }}>
      <Card className="glass-card" style={{ width: 480 }}>
        <Title level={3}>完成注册</Title>
        <Text>
          你正在通过 <b>{providerLabel}</b> 首次登录，请设置密码并完成账号绑定。
        </Text>

        <div style={{ marginTop: 16, marginBottom: 16 }}>
          <Text type="secondary">账号邮箱：{email}</Text>
          <br />
          <Text type="secondary">用户昵称：{nickname}</Text>
        </div>

        <Form
          form={form}
          layout="vertical"
          onFinish={async (values) => {
            const data = await oauthRegister(registerKey, values.password)
            if (!data) {
              message.error('补注册失败，请重试')
              return
            }
            setUser({
              ...data.userInfo,
              token: data.token,
              refreshToken: data.refreshToken,
            })
            message.success('绑定成功')
            navigate('/main/all')
          }}
        >
          <Form.Item
            label="设置密码"
            name="password"
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
            <Input.Password placeholder="输入新密码" />
          </Form.Item>

          <Form.Item
            label="确认密码"
            name="confirmPassword"
            dependencies={['password']}
            rules={[
              { required: true, message: '请确认密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || value === getFieldValue('password')) {
                    return Promise.resolve()
                  }
                  return Promise.reject(new Error('两次输入不一致'))
                },
              }),
            ]}
          >
            <Input.Password placeholder="再次输入密码" />
          </Form.Item>

          <Button type="primary" htmlType="submit" block>
            完成注册并登录
          </Button>
        </Form>
      </Card>
    </div>
  )
}

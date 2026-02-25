import { useEffect } from 'react'
import { App, Card, Spin, Typography } from 'antd'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { qqLoginCallback } from '@/services/account-service'
import { useAuthStore } from '@/store/auth-store'

const { Text } = Typography

export const QqCallbackPage = () => {
  const { message } = App.useApp()
  const navigate = useNavigate()
  const [search] = useSearchParams()
  const setUser = useAuthStore((state) => state.setUser)

  useEffect(() => {
    const run = async () => {
      const params = Object.fromEntries(search.entries())
      const data = await qqLoginCallback(params)
      if (!data) {
        message.error('QQ 登录失败')
        navigate('/login')
        return
      }

      setUser({
        ...data.userInfo,
        token: data.token,
        refreshToken: data.refreshToken,
      })
      navigate(data.redirectUrl && data.redirectUrl !== '/login' ? data.redirectUrl : '/main/all')
    }

    void run()
  }, [message, navigate, search, setUser])

  return (
    <div className="auth-page app-page" style={{ display: 'grid', placeItems: 'center' }}>
      <Card className="glass-card" style={{ textAlign: 'center', minWidth: 320 }}>
        <Spin />
        <Text style={{ display: 'block', marginTop: 10 }}>正在处理 QQ 登录...</Text>
      </Card>
    </div>
  )
}


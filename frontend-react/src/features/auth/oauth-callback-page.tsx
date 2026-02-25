import { useEffect } from 'react'
import { App, Card, Spin, Typography } from 'antd'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { oauthCallback } from '@/services/oauth-service'
import { useAuthStore } from '@/store/auth-store'

const { Text } = Typography

export const OAuthCallbackPage = () => {
  const { message } = App.useApp()
  const navigate = useNavigate()
  const { provider } = useParams<{ provider: string }>()
  const [search] = useSearchParams()
  const setUser = useAuthStore((state) => state.setUser)

  useEffect(() => {
    const run = async () => {
      if (!provider) {
        navigate('/login')
        return
      }

      const data = await oauthCallback(provider, Object.fromEntries(search.entries()))
      if (!data) {
        message.error('OAuth 登录失败')
        navigate('/login')
        return
      }

      if (data.status === 'need_register') {
        const query = new URLSearchParams({
          registerKey: data.registerKey,
          email: data.email || '',
          nickname: data.nickname || '',
          avatarUrl: data.avatarUrl || '',
          provider: data.provider || provider,
        }).toString()
        navigate(`/oauth/register?${query}`)
        return
      }

      setUser({
        ...data.userInfo,
        token: data.token,
        refreshToken: data.refreshToken,
      })
      const callbackUrl = data.callbackUrl && !data.callbackUrl.includes('/login') ? data.callbackUrl : '/main/all'
      navigate(callbackUrl)
    }

    void run()
  }, [message, navigate, provider, search, setUser])

  return (
    <div className="auth-page app-page" style={{ display: 'grid', placeItems: 'center' }}>
      <Card className="glass-card" style={{ minWidth: 320, textAlign: 'center' }}>
        <Spin />
        <Text style={{ display: 'block', marginTop: 10 }}>正在处理第三方登录...</Text>
      </Card>
    </div>
  )
}


import { StrictMode, useEffect, useState } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ConfigProvider, App as AntdApp, Spin } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import { appRouter } from '@/app/router'
import { initWebVitals } from '@/services/analytics-service'
import { getUserInfo } from '@/services/account-service'
import { useAuthStore } from '@/store/auth-store'
import '@/styles/global.css'
import '@/styles/experience.css'

dayjs.locale('zh-cn')

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 3000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

initWebVitals()

const AuthBootstrap = () => {
  const [ready, setReady] = useState(false)
  const user = useAuthStore((state) => state.user)
  const setUser = useAuthStore((state) => state.setUser)

  useEffect(() => {
    let disposed = false
    const run = async () => {
      if (user) {
        if (!disposed) {
          setReady(true)
        }
        return
      }

      let timeoutId = 0
      const backendUser = await Promise.race<Awaited<ReturnType<typeof getUserInfo>>>([
        getUserInfo({ skipAuthRefresh: true }),
        new Promise((resolve) => {
          timeoutId = window.setTimeout(() => resolve(null), 1500)
        }),
      ])
      window.clearTimeout(timeoutId)
      if (!disposed && backendUser) {
        const cached = useAuthStore.getState().user
        setUser({
          ...backendUser,
          token: cached?.token,
          refreshToken: cached?.refreshToken,
        })
      }
      if (!disposed) {
        setReady(true)
      }
    }

    void run()
    return () => {
      disposed = true
    }
  }, [setUser, user])

  if (!ready) {
    return (
      <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}>
        <Spin size="large" />
      </div>
    )
  }

  return <RouterProvider router={appRouter} />
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ConfigProvider
      locale={zhCN}
      theme={{
        token: {
          colorPrimary: '#166b88',
          borderRadius: 14,
          fontFamily: "'Noto Sans SC', 'Segoe UI', sans-serif",
        },
      }}
    >
      <AntdApp>
        <QueryClientProvider client={queryClient}>
          <AuthBootstrap />
        </QueryClientProvider>
      </AntdApp>
    </ConfigProvider>
  </StrictMode>
)


import { useEffect } from 'react'
import { Layout } from 'antd'
import { Outlet, useLocation } from 'react-router-dom'
import { AppHeader } from '@/components/layout/app-header'
import { SideNav } from '@/components/layout/side-nav'
import { prefetchPrivateRoutes } from '@/app/route-prefetch'
import { useUiStore } from '@/store/ui-store'
import './layout.css'

const { Content, Sider } = Layout

export const AppShell = () => {
  const location = useLocation()
  const mobileMenuOpen = useUiStore((state) => state.mobileMenuOpen)
  const setMobileMenuOpen = useUiStore((state) => state.setMobileMenuOpen)

  useEffect(() => {
    setMobileMenuOpen(false)
  }, [location.pathname, location.search, setMobileMenuOpen])

  useEffect(() => {
    if (!mobileMenuOpen) {
      return
    }
    const originalOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = originalOverflow
    }
  }, [mobileMenuOpen])

  useEffect(() => {
    const win = window as unknown as {
      requestIdleCallback?: (callback: () => void, options?: { timeout: number }) => number
      cancelIdleCallback?: (id: number) => void
    }

    let timerId: number | null = null
    let idleId: number | null = null

    const runPrefetch = () => {
      void prefetchPrivateRoutes()
    }

    if (typeof win.requestIdleCallback === 'function') {
      idleId = win.requestIdleCallback(runPrefetch, { timeout: 1800 })
    } else {
      timerId = window.setTimeout(runPrefetch, 600)
    }

    return () => {
      if (timerId !== null) {
        window.clearTimeout(timerId)
      }
      if (idleId !== null && typeof win.cancelIdleCallback === 'function') {
        win.cancelIdleCallback(idleId)
      }
    }
  }, [])

  return (
    <Layout className="app-shell app-page">
      <AppHeader />
      <Layout className="app-shell-body">
        <Sider
          width={264}
          className={`app-shell-sider ${mobileMenuOpen ? 'open' : ''}`}
          breakpoint="md"
          collapsedWidth={0}
          trigger={null}
        >
          <SideNav />
        </Sider>
        <div className={`app-shell-mask ${mobileMenuOpen ? 'show' : ''}`} onClick={() => setMobileMenuOpen(false)} />
        <Content className="app-shell-content">
          <div key={`${location.pathname}${location.search}`} className="app-content-inner page-enter">
            <Outlet />
          </div>
        </Content>
      </Layout>
    </Layout>
  )
}


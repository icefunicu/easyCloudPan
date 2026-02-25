import { Suspense, lazy, type ReactNode } from 'react'
import { Navigate, createBrowserRouter } from 'react-router-dom'
import { RouteGuard } from '@/app/route-guard'

const AppShell = lazy(async () => ({ default: (await import('@/components/layout/app-shell')).AppShell }))
const LoginPage = lazy(async () => ({ default: (await import('@/features/auth/login-page')).LoginPage }))
const OAuthCallbackPage = lazy(async () => ({
  default: (await import('@/features/auth/oauth-callback-page')).OAuthCallbackPage,
}))
const OAuthRegisterPage = lazy(async () => ({
  default: (await import('@/features/auth/oauth-register-page')).OAuthRegisterPage,
}))
const QqCallbackPage = lazy(async () => ({ default: (await import('@/features/auth/qq-callback-page')).QqCallbackPage }))
const MainPage = lazy(async () => ({ default: (await import('@/features/main/main-page')).MainPage }))
const MySharePage = lazy(async () => ({ default: (await import('@/features/share/my-share-page')).MySharePage }))
const RecyclePage = lazy(async () => ({ default: (await import('@/features/recycle/recycle-page')).RecyclePage }))
const AdminUserListPage = lazy(async () => ({
  default: (await import('@/features/admin/admin-user-list-page')).AdminUserListPage,
}))
const AdminFileListPage = lazy(async () => ({
  default: (await import('@/features/admin/admin-file-list-page')).AdminFileListPage,
}))
const AdminSysSettingsPage = lazy(async () => ({
  default: (await import('@/features/admin/admin-sys-settings-page')).AdminSysSettingsPage,
}))
const ShareCheckPage = lazy(async () => ({
  default: (await import('@/features/webshare/share-check-page')).ShareCheckPage,
}))
const ShareBrowsePage = lazy(async () => ({
  default: (await import('@/features/webshare/share-browse-page')).ShareBrowsePage,
}))
const FilePreviewPage = lazy(async () => ({
  default: (await import('@/features/main/file-preview-page')).FilePreviewPage,
}))
const NotFoundPage = lazy(async () => ({ default: (await import('@/features/main/not-found-page')).NotFoundPage }))

const lazyElement = (element: ReactNode) => (
  <Suspense
    fallback={
      <div className="route-loader">
        <div className="route-loader-panel">
          <div className="route-loader-head">
            <span className="route-loader-badge">云</span>
            <div>
              <p className="route-loader-title">EasyCloudPan 正在加载</p>
              <p className="route-loader-text">页面资源已进入高速渲染通道</p>
            </div>
          </div>
          <div className="route-loader-line" />
          <div className="route-loader-line" />
          <div className="route-loader-line" />
        </div>
      </div>
    }
  >
    {element}
  </Suspense>
)

export const appRouter = createBrowserRouter([
  {
    path: '/login',
    element: lazyElement(<LoginPage />),
  },
  {
    path: '/qqlogincallback',
    element: lazyElement(<QqCallbackPage />),
  },
  {
    path: '/oauth/callback/:provider',
    element: lazyElement(<OAuthCallbackPage />),
  },
  {
    path: '/oauth/register',
    element: lazyElement(<OAuthRegisterPage />),
  },
  {
    path: '/shareCheck/:shareId',
    element: lazyElement(<ShareCheckPage />),
  },
  {
    path: '/share/:shareId',
    element: lazyElement(<ShareBrowsePage />),
  },
  {
    path: '/preview/:fileId',
    element: lazyElement(<FilePreviewPage />),
  },
  {
    element: <RouteGuard />,
    children: [
      {
        path: '/',
        element: lazyElement(<AppShell />),
        children: [
          { index: true, element: <Navigate to="/main/all" replace /> },
          { path: '/main/:category', element: lazyElement(<MainPage />) },
          { path: '/myshare', element: lazyElement(<MySharePage />) },
          { path: '/recycle', element: lazyElement(<RecyclePage />) },
          {
            element: <RouteGuard requireAdmin />,
            children: [
              { path: '/settings/userList', element: lazyElement(<AdminUserListPage />) },
              { path: '/settings/fileList', element: lazyElement(<AdminFileListPage />) },
              { path: '/settings/sysSetting', element: lazyElement(<AdminSysSettingsPage />) },
            ],
          },
        ],
      },
    ],
  },
  {
    path: '*',
    element: lazyElement(<NotFoundPage />),
  },
])


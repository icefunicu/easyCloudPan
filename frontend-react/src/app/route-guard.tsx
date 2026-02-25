import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/store/auth-store'

interface GuardProps {
  requireAdmin?: boolean
}

export const RouteGuard = ({ requireAdmin = false }: GuardProps) => {
  const location = useLocation()
  const user = useAuthStore((state) => state.user)

  if (!user) {
    return <Navigate to={`/login?redirectUrl=${encodeURIComponent(location.pathname + location.search)}`} replace />
  }

  if (requireAdmin && !user.admin) {
    return <Navigate to="/main/all" replace />
  }

  return <Outlet />
}


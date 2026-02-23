import { Navigate, Outlet } from 'react-router'
import { useAuth } from '@/hooks/useAuth'
import type { Role } from '@/types'

interface RoleProtectedRouteProps {
  allowedRoles: Role[]
}

/**
 * ロールベースでルートを保護するコンポーネント
 * 権限不足の場合は /403 を表示する
 */
export function RoleProtectedRoute({ allowedRoles }: RoleProtectedRouteProps) {
  const { user, isLoading } = useAuth()

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (!allowedRoles.includes(user.role)) {
    return <Navigate to="/403" replace />
  }

  return <Outlet />
}

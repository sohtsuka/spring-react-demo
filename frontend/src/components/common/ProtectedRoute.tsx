import { Navigate, Outlet } from 'react-router'
import { useAuth } from '@/hooks/useAuth'

/**
 * 認証が必要なルートを保護するコンポーネント
 * 未認証の場合は /login にリダイレクトする
 */
export function ProtectedRoute() {
  const { isAuthenticated, isLoading } = useAuth()

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return <Outlet />
}

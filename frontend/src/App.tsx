import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { Layout } from '@/components/common/Layout'
import { ProtectedRoute } from '@/components/common/ProtectedRoute'
import { RoleProtectedRoute } from '@/components/common/RoleProtectedRoute'
import { Toaster } from '@/components/ui/toaster'
import { AdminPage } from '@/pages/admin/AdminPage'
import { DashboardPage } from '@/pages/DashboardPage'
import { ForbiddenPage } from '@/pages/ForbiddenPage'
import { LoginPage } from '@/pages/LoginPage'
import { ManagerPage } from '@/pages/manager/ManagerPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { ProfilePage } from '@/pages/ProfilePage'

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* 認証不要 */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/403" element={<ForbiddenPage />} />

        {/* 認証必須 */}
        <Route element={<ProtectedRoute />}>
          <Route element={<Layout />}>
            {/* 全ロール共通 */}
            <Route index element={<DashboardPage />} />
            <Route path="profile" element={<ProfilePage />} />

            {/* ADMIN / MANAGER のみ */}
            <Route element={<RoleProtectedRoute allowedRoles={['ADMIN', 'MANAGER']} />}>
              <Route path="manager/*" element={<ManagerPage />} />
            </Route>

            {/* ADMIN のみ */}
            <Route element={<RoleProtectedRoute allowedRoles={['ADMIN']} />}>
              <Route path="admin/*" element={<AdminPage />} />
            </Route>
          </Route>
        </Route>

        {/* 404 */}
        <Route path="/404" element={<NotFoundPage />} />
        <Route path="*" element={<Navigate to="/404" replace />} />
      </Routes>
      <Toaster />
    </BrowserRouter>
  )
}

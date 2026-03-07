import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Outlet, Route, Routes } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import type { AuthUser } from '@/types'
import { Layout } from '../Layout'

vi.mock('@/hooks/useAuth')
import { useAuth } from '@/hooks/useAuth'
const mockUseAuth = vi.mocked(useAuth)

function makeAuthReturn(overrides: Partial<ReturnType<typeof useAuth>>) {
  return {
    isLoading: false,
    isAuthenticated: true,
    user: undefined as AuthUser | undefined,
    login: vi.fn(),
    loginAsync: vi.fn(),
    loginError: null,
    isLoggingIn: false,
    logout: vi.fn(),
    isLoggingOut: false,
    ...overrides,
  }
}

function renderLayout(initialEntry = '/', user?: AuthUser) {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<div>dashboard</div>} />
          <Route path="/profile" element={<div>profile</div>} />
          <Route path="/manager" element={<div>manager</div>} />
          <Route path="/admin" element={<div>admin</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  )
}

describe('Layout: ナビゲーション表示', () => {
  it('user が undefined の場合イニシャルは ??', () => {
    mockUseAuth.mockReturnValue(makeAuthReturn({ user: undefined }))
    renderLayout()
    expect(screen.getByText('??')).toBeInTheDocument()
  })

  it('user.username がある場合イニシャルは先頭2文字大文字', () => {
    mockUseAuth.mockReturnValue(
      makeAuthReturn({ user: { id: 1, username: 'alice', email: 'a@a.com', role: 'USER' } }),
    )
    renderLayout()
    expect(screen.getByText('AL')).toBeInTheDocument()
  })

  it('roles なし navItem（ダッシュボード・プロフィール）は常に表示', () => {
    mockUseAuth.mockReturnValue(makeAuthReturn({ user: undefined }))
    renderLayout()
    expect(screen.getByText('ダッシュボード')).toBeInTheDocument()
    expect(screen.getByText('プロフィール')).toBeInTheDocument()
  })

  it('user が undefined のとき roles ありの navItem（マネージャー・管理者）は表示しない', () => {
    mockUseAuth.mockReturnValue(makeAuthReturn({ user: undefined }))
    renderLayout()
    expect(screen.queryByText('マネージャー')).toBeNull()
    expect(screen.queryByText('管理者')).toBeNull()
  })

  it('MANAGER ロールのとき「マネージャー」は表示、「管理者」は非表示', () => {
    mockUseAuth.mockReturnValue(
      makeAuthReturn({ user: { id: 1, username: 'mgr', email: 'm@m.com', role: 'MANAGER' } }),
    )
    renderLayout()
    expect(screen.getByText('マネージャー')).toBeInTheDocument()
    expect(screen.queryByText('管理者')).toBeNull()
  })

  it('ADMIN ロールのとき「マネージャー」「管理者」ともに表示', () => {
    mockUseAuth.mockReturnValue(
      makeAuthReturn({ user: { id: 1, username: 'adm', email: 'a@a.com', role: 'ADMIN' } }),
    )
    renderLayout()
    expect(screen.getByText('マネージャー')).toBeInTheDocument()
    expect(screen.getByText('管理者')).toBeInTheDocument()
  })

  it('パスが "/" のとき「ダッシュボード」が active クラスを持つ', () => {
    mockUseAuth.mockReturnValue(makeAuthReturn({ user: undefined }))
    renderLayout('/')
    const link = screen.getByText('ダッシュボード').closest('a')
    expect(link?.className).toContain('bg-primary')
  })

  it('パスが "/profile" のとき「プロフィール」が active、「ダッシュボード」は inactive', () => {
    mockUseAuth.mockReturnValue(makeAuthReturn({ user: undefined }))
    renderLayout('/profile')
    const profileLink = screen.getByText('プロフィール').closest('a')
    const dashLink = screen.getByText('ダッシュボード').closest('a')
    expect(profileLink?.className).toContain('bg-primary')
    expect(dashLink?.className).not.toContain('bg-primary')
  })

  it('ログアウトボタンをクリックすると logout() が呼ばれる', async () => {
    const logout = vi.fn()
    mockUseAuth.mockReturnValue(makeAuthReturn({ user: undefined, logout }))
    renderLayout()
    await userEvent.click(screen.getByTitle('ログアウト'))
    expect(logout).toHaveBeenCalledOnce()
  })

  it('isLoggingOut: true のときログアウトボタンが disabled', () => {
    mockUseAuth.mockReturnValue(makeAuthReturn({ user: undefined, isLoggingOut: true }))
    renderLayout()
    expect(screen.getByTitle('ログアウト')).toBeDisabled()
  })
})

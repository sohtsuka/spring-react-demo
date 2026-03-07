import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import type { AuthUser } from '@/types'
import { RoleProtectedRoute } from '../RoleProtectedRoute'

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

function renderRoute(allowedRoles: string[], initialEntry = '/') {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route path="/login" element={<div>login page</div>} />
        <Route path="/403" element={<div>forbidden page</div>} />
        <Route element={<RoleProtectedRoute allowedRoles={allowedRoles as never} />}>
          <Route path="/" element={<div>role content</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  )
}

describe('RoleProtectedRoute', () => {
  it('isLoading: true のときスピナーを表示する', () => {
    mockUseAuth.mockReturnValue(makeAuthReturn({ isLoading: true }))
    renderRoute(['ADMIN'])
    expect(document.querySelector('.animate-spin')).not.toBeNull()
  })

  it('user: null のとき /login へリダイレクト', () => {
    mockUseAuth.mockReturnValue(makeAuthReturn({ user: undefined }))
    renderRoute(['ADMIN'])
    expect(screen.getByText('login page')).toBeInTheDocument()
  })

  it('user.role が allowedRoles に含まれない → /403 へリダイレクト', () => {
    mockUseAuth.mockReturnValue(
      makeAuthReturn({ user: { id: 1, username: 'u', email: 'e', role: 'USER' } }),
    )
    renderRoute(['ADMIN'])
    expect(screen.getByText('forbidden page')).toBeInTheDocument()
  })

  it('user.role が allowedRoles に含まれる → Outlet を描画する', () => {
    mockUseAuth.mockReturnValue(
      makeAuthReturn({ user: { id: 1, username: 'u', email: 'e', role: 'ADMIN' } }),
    )
    renderRoute(['ADMIN'])
    expect(screen.getByText('role content')).toBeInTheDocument()
  })
})

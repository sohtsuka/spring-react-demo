import { render, screen } from '@testing-library/react'
import { MemoryRouter, Outlet, Route, Routes } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { ProtectedRoute } from '../ProtectedRoute'

vi.mock('@/hooks/useAuth')
import { useAuth } from '@/hooks/useAuth'
const mockUseAuth = vi.mocked(useAuth)

function TestOutlet() {
  return <div>protected content</div>
}

function renderRoute(initialEntry = '/') {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route path="/login" element={<div>login page</div>} />
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<TestOutlet />} />
        </Route>
      </Routes>
    </MemoryRouter>,
  )
}

describe('ProtectedRoute', () => {
  it('isLoading: true のときスピナーを表示する', () => {
    mockUseAuth.mockReturnValue({
      isLoading: true,
      isAuthenticated: false,
      user: undefined,
      login: vi.fn(),
      loginAsync: vi.fn(),
      loginError: null,
      isLoggingIn: false,
      logout: vi.fn(),
      isLoggingOut: false,
    })
    renderRoute()
    expect(document.querySelector('.animate-spin')).not.toBeNull()
  })

  it('isAuthenticated: false のとき /login へリダイレクト', () => {
    mockUseAuth.mockReturnValue({
      isLoading: false,
      isAuthenticated: false,
      user: undefined,
      login: vi.fn(),
      loginAsync: vi.fn(),
      loginError: null,
      isLoggingIn: false,
      logout: vi.fn(),
      isLoggingOut: false,
    })
    renderRoute()
    expect(screen.getByText('login page')).toBeInTheDocument()
  })

  it('isAuthenticated: true のとき Outlet を描画する', () => {
    mockUseAuth.mockReturnValue({
      isLoading: false,
      isAuthenticated: true,
      user: { id: 1, username: 'u', email: 'e', role: 'USER' },
      login: vi.fn(),
      loginAsync: vi.fn(),
      loginError: null,
      isLoggingIn: false,
      logout: vi.fn(),
      isLoggingOut: false,
    })
    renderRoute()
    expect(screen.getByText('protected content')).toBeInTheDocument()
  })
})

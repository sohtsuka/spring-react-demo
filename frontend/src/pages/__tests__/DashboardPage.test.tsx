import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { DashboardPage } from '../DashboardPage'

vi.mock('@/hooks/useAuth')
import { useAuth } from '@/hooks/useAuth'
const mockUseAuth = vi.mocked(useAuth)

function makeAuth(overrides: Partial<ReturnType<typeof useAuth>>) {
  return {
    isLoading: false,
    isAuthenticated: true,
    user: undefined,
    login: vi.fn(),
    loginAsync: vi.fn(),
    loginError: null,
    isLoggingIn: false,
    logout: vi.fn(),
    isLoggingOut: false,
    ...overrides,
  }
}

describe('DashboardPage', () => {
  it('user が存在する場合、ユーザー名・メール・ロールを表示する', () => {
    mockUseAuth.mockReturnValue(
      makeAuth({ user: { id: 1, username: 'alice', email: 'alice@example.com', role: 'ADMIN' } }),
    )
    render(<MemoryRouter><DashboardPage /></MemoryRouter>)
    expect(screen.getByText('ようこそ、alice さん')).toBeInTheDocument()
    expect(screen.getByText('alice@example.com')).toBeInTheDocument()
    expect(screen.getByText('ADMIN')).toBeInTheDocument()
  })

  it('user が undefined の場合でもクラッシュしない', () => {
    mockUseAuth.mockReturnValue(makeAuth({ user: undefined }))
    render(<MemoryRouter><DashboardPage /></MemoryRouter>)
    expect(screen.getByText('ダッシュボード')).toBeInTheDocument()
  })
})

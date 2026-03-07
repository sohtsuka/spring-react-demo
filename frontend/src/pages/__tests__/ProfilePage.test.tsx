import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { ProfilePage } from '../ProfilePage'

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

describe('ProfilePage', () => {
  it('user が存在する場合、全フィールドを表示する', () => {
    mockUseAuth.mockReturnValue(
      makeAuth({ user: { id: 42, username: 'bob', email: 'bob@example.com', role: 'MANAGER' } }),
    )
    render(<MemoryRouter><ProfilePage /></MemoryRouter>)
    expect(screen.getByText('42')).toBeInTheDocument()
    expect(screen.getByText('bob')).toBeInTheDocument()
    expect(screen.getByText('bob@example.com')).toBeInTheDocument()
    expect(screen.getByText('MANAGER')).toBeInTheDocument()
  })

  it('user が undefined の場合でもクラッシュしない', () => {
    mockUseAuth.mockReturnValue(makeAuth({ user: undefined }))
    render(<MemoryRouter><ProfilePage /></MemoryRouter>)
    expect(screen.getByText('プロフィール')).toBeInTheDocument()
  })
})

import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { HttpError } from '@/lib/api'
import { LoginPage } from '../LoginPage'

vi.mock('@/hooks/useAuth')
import { useAuth } from '@/hooks/useAuth'
const mockUseAuth = vi.mocked(useAuth)

function makeAuth(overrides: Partial<ReturnType<typeof useAuth>>) {
  return {
    isLoading: false,
    isAuthenticated: false,
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

function renderLoginPage(initialEntry = '/login') {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route path="/" element={<div>dashboard</div>} />
        <Route path="/login" element={<LoginPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('LoginPage', () => {
  it('isAuthenticated: true → ダッシュボードへ遷移する', async () => {
    mockUseAuth.mockReturnValue(makeAuth({ isAuthenticated: true }))
    renderLoginPage()
    await waitFor(() => {
      expect(screen.getByText('dashboard')).toBeInTheDocument()
    })
  })

  it('isAuthenticated: false → ログインページを表示する', () => {
    mockUseAuth.mockReturnValue(makeAuth({ isAuthenticated: false }))
    renderLoginPage()
    expect(screen.getByRole('heading', { name: 'ログイン' })).toBeInTheDocument()
  })

  it('isLoggingIn: true → ボタンに「ログイン中...」を表示する', () => {
    mockUseAuth.mockReturnValue(makeAuth({ isLoggingIn: true }))
    renderLoginPage()
    expect(screen.getByRole('button', { name: 'ログイン中...' })).toBeDisabled()
  })

  it('isLoggingIn: false → ボタンに「ログイン」を表示する', () => {
    mockUseAuth.mockReturnValue(makeAuth({ isLoggingIn: false }))
    renderLoginPage()
    expect(screen.getByRole('button', { name: 'ログイン' })).not.toBeDisabled()
  })

  it('バリデーション: username 空 → フィールドエラーを表示する', async () => {
    mockUseAuth.mockReturnValue(makeAuth({}))
    renderLoginPage()
    await userEvent.type(screen.getByLabelText('パスワード'), 'Password123')
    await userEvent.click(screen.getByRole('button', { name: 'ログイン' }))
    await waitFor(() => {
      expect(screen.getByText('ユーザー名を入力してください')).toBeInTheDocument()
    })
  })

  it('バリデーション: password 短すぎる → フィールドエラーを表示する', async () => {
    mockUseAuth.mockReturnValue(makeAuth({}))
    renderLoginPage()
    await userEvent.type(screen.getByLabelText('ユーザー名'), 'user')
    await userEvent.type(screen.getByLabelText('パスワード'), 'short')
    await userEvent.click(screen.getByRole('button', { name: 'ログイン' }))
    await waitFor(() => {
      expect(screen.getByText('パスワードは8文字以上で入力してください')).toBeInTheDocument()
    })
  })

  it('フォーム送信: 401 → 認証失敗メッセージを表示する', async () => {
    const loginAsync = vi.fn().mockRejectedValue(new HttpError(401, 'Unauthorized'))
    mockUseAuth.mockReturnValue(makeAuth({ loginAsync }))
    renderLoginPage()
    await userEvent.type(screen.getByLabelText('ユーザー名'), 'user')
    await userEvent.type(screen.getByLabelText('パスワード'), 'Password123')
    await userEvent.click(screen.getByRole('button', { name: 'ログイン' }))
    await waitFor(() => {
      expect(
        screen.getByText('ユーザー名またはパスワードが正しくありません'),
      ).toBeInTheDocument()
    })
  })

  it('フォーム送信: 423 → アカウントロックメッセージを表示する', async () => {
    const loginAsync = vi.fn().mockRejectedValue(new HttpError(423, 'Locked'))
    mockUseAuth.mockReturnValue(makeAuth({ loginAsync }))
    renderLoginPage()
    await userEvent.type(screen.getByLabelText('ユーザー名'), 'user')
    await userEvent.type(screen.getByLabelText('パスワード'), 'Password123')
    await userEvent.click(screen.getByRole('button', { name: 'ログイン' }))
    await waitFor(() => {
      expect(screen.getByText(/アカウントがロックされています/)).toBeInTheDocument()
    })
  })

  it('フォーム送信: その他エラー → 汎用エラーメッセージを表示する', async () => {
    const loginAsync = vi.fn().mockRejectedValue(new HttpError(500, 'Server Error'))
    mockUseAuth.mockReturnValue(makeAuth({ loginAsync }))
    renderLoginPage()
    await userEvent.type(screen.getByLabelText('ユーザー名'), 'user')
    await userEvent.type(screen.getByLabelText('パスワード'), 'Password123')
    await userEvent.click(screen.getByRole('button', { name: 'ログイン' }))
    await waitFor(() => {
      expect(screen.getByText('ログインに失敗しました')).toBeInTheDocument()
    })
  })

  it('フォーム送信: 非 Axios エラー → エラーハンドリングをスキップする（クラッシュしない）', async () => {
    const loginAsync = vi.fn().mockRejectedValue(new Error('network error'))
    mockUseAuth.mockReturnValue(makeAuth({ loginAsync }))
    renderLoginPage()
    await userEvent.type(screen.getByLabelText('ユーザー名'), 'user')
    await userEvent.type(screen.getByLabelText('パスワード'), 'Password123')
    await userEvent.click(screen.getByRole('button', { name: 'ログイン' }))
    await waitFor(() => {
      expect(loginAsync).toHaveBeenCalled()
    })
    // 非 Axios エラーの場合はエラーメッセージを表示しない（クラッシュもしない）
    expect(screen.queryByText('ログインに失敗しました')).not.toBeInTheDocument()
  })

  it('フォーム送信: 成功 → loginAsync が呼ばれる', async () => {
    const loginAsync = vi.fn().mockResolvedValue(undefined)
    mockUseAuth.mockReturnValue(makeAuth({ loginAsync }))
    renderLoginPage()
    await userEvent.type(screen.getByLabelText('ユーザー名'), 'user')
    await userEvent.type(screen.getByLabelText('パスワード'), 'Password123')
    await userEvent.click(screen.getByRole('button', { name: 'ログイン' }))
    await waitFor(() => {
      expect(loginAsync).toHaveBeenCalledWith({ username: 'user', password: 'Password123' })
    })
  })
})

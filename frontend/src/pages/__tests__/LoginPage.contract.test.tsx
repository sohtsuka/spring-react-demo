import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { MemoryRouter, Route, Routes } from 'react-router'
import { describe, expect, it } from 'vitest'
import { server } from '@/test/server'
import { LoginPage } from '../LoginPage'

describe('ログイン API と画面のエラー契約', () => {
  it.each([
    [401, 'ACCOUNT_LOCKED', 'アカウントがロックされています'],
    [401, 'ACCOUNT_DISABLED', 'アカウントが無効化されています'],
    [429, 'RATE_LIMIT_EXCEEDED', 'リクエストが多すぎます'],
  ])('%i %s を画面に表示する', async (status, code, message) => {
    server.use(
      http.get('/api/v1/auth/me', () => new HttpResponse(null, { status: 401 })),
      http.post('/api/v1/auth/login', () =>
        HttpResponse.json({ code, message, details: [], timestamp: '2026-09-27T00:00:00Z' }, { status }),
      ),
    )
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/login']}>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    )

    await userEvent.type(screen.getByLabelText('ユーザー名'), 'user')
    await userEvent.type(screen.getByLabelText('パスワード'), 'Password123')
    await userEvent.click(screen.getByRole('button', { name: 'ログイン' }))
    expect(await screen.findByText(new RegExp(message))).toBeInTheDocument()
  })
})

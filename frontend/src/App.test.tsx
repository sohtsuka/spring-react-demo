import { http, HttpResponse } from 'msw'
import { render, screen, waitFor } from '@testing-library/react'
import { QueryClientProvider } from '@tanstack/react-query'
import { describe, expect, it } from 'vitest'
import { mockAdminUser, mockAuthUser, mockManagerUser } from '@/test/mockUser'
import { server } from '@/test/server'
import { createTestQueryClient } from '@/test/renderWithProviders'
import { App } from './App'

function renderApp(initialPath = '/') {
  window.history.pushState({}, '', initialPath)
  const qc = createTestQueryClient()
  return render(
    <QueryClientProvider client={qc}>
      <App />
    </QueryClientProvider>,
  )
}

describe('App ルーティング', () => {
  it('/login → LoginPage を表示する', async () => {
    server.use(
      http.get('/api/v1/auth/me', () => new HttpResponse(null, { status: 401 })),
    )
    renderApp('/login')
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'ログイン' })).toBeInTheDocument()
    })
  })

  it('/403 → ForbiddenPage を表示する', async () => {
    server.use(
      http.get('/api/v1/auth/me', () => new HttpResponse(null, { status: 401 })),
    )
    renderApp('/403')
    await waitFor(() => {
      expect(screen.getByText('403')).toBeInTheDocument()
    })
  })

  it('/404 → NotFoundPage を表示する', async () => {
    server.use(
      http.get('/api/v1/auth/me', () => new HttpResponse(null, { status: 401 })),
    )
    renderApp('/404')
    await waitFor(() => {
      expect(screen.getByText('404')).toBeInTheDocument()
    })
  })

  it('未知パス → /404 にリダイレクトして NotFoundPage を表示する', async () => {
    server.use(
      http.get('/api/v1/auth/me', () => new HttpResponse(null, { status: 401 })),
    )
    renderApp('/unknown-route')
    await waitFor(() => {
      expect(screen.getByText('404')).toBeInTheDocument()
    })
  })

  it('未認証 + / → /login にリダイレクトする', async () => {
    server.use(
      http.get('/api/v1/auth/me', () => new HttpResponse(null, { status: 401 })),
    )
    renderApp('/')
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'ログイン' })).toBeInTheDocument()
    })
  })

  it('認証済み + / → DashboardPage を表示する', async () => {
    renderApp('/')
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'ダッシュボード' })).toBeInTheDocument()
    })
  })

  it('認証済み + /profile → ProfilePage を表示する', async () => {
    renderApp('/profile')
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'プロフィール' })).toBeInTheDocument()
    })
  })

  it('ADMIN ロール + /admin → AdminPage を表示する', async () => {
    server.use(
      http.get('/api/v1/auth/me', () => HttpResponse.json({ data: mockAdminUser })),
    )
    renderApp('/admin')
    await waitFor(() => {
      expect(screen.getByText('管理者画面')).toBeInTheDocument()
    })
  })

  it('MANAGER ロール + /manager → ManagerPage を表示する', async () => {
    server.use(
      http.get('/api/v1/auth/me', () => HttpResponse.json({ data: mockManagerUser })),
    )
    renderApp('/manager')
    await waitFor(() => {
      expect(screen.getByText('マネージャー画面')).toBeInTheDocument()
    })
  })

  it('USER ロール + /admin → /403 にリダイレクトする', async () => {
    renderApp('/admin')
    await waitFor(() => {
      expect(screen.getByText('アクセス権限がありません')).toBeInTheDocument()
    })
  })
})

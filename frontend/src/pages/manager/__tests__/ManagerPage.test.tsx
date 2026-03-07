import { http, HttpResponse } from 'msw'
import { render, screen, waitFor } from '@testing-library/react'
import { QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import { mockDisabledUser, mockUser } from '@/test/mockUser'
import { server } from '@/test/server'
import { createTestQueryClient } from '@/test/renderWithProviders'
import { ManagerPage } from '../ManagerPage'

function renderManagerPage() {
  const qc = createTestQueryClient()
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter>
        <ManagerPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ManagerPage', () => {
  it('ローディング中はスピナーを表示する', () => {
    // サーバーの応答を遅延させる
    server.use(
      http.get('/api/v1/users', async () => {
        await new Promise((r) => setTimeout(r, 500))
        return HttpResponse.json({ data: [], pagination: { page: 1, size: 20, totalElements: 0, totalPages: 0 } })
      }),
    )
    renderManagerPage()
    expect(document.querySelector('.animate-spin')).not.toBeNull()
  })

  it('エラー時はエラーメッセージを表示する', async () => {
    server.use(
      http.get('/api/v1/users', () => new HttpResponse(null, { status: 500 })),
    )
    renderManagerPage()
    await waitFor(() => {
      expect(screen.getByText('データの読み込みに失敗しました')).toBeInTheDocument()
    })
  })

  it('データ取得成功時はテーブルを表示する', async () => {
    renderManagerPage()
    await waitFor(() => {
      expect(screen.getByText('testuser')).toBeInTheDocument()
    })
    expect(screen.getByText('test@example.com')).toBeInTheDocument()
  })

  it('user.enabled: true → バッジに「有効」を表示する', async () => {
    renderManagerPage()
    await waitFor(() => expect(screen.getByText('有効')).toBeInTheDocument())
  })

  it('user.enabled: false → バッジに「無効」を表示する', async () => {
    server.use(
      http.get('/api/v1/users', () =>
        HttpResponse.json({
          data: [mockDisabledUser],
          pagination: { page: 1, size: 20, totalElements: 1, totalPages: 1 },
        }),
      ),
    )
    renderManagerPage()
    await waitFor(() => expect(screen.getByText('無効')).toBeInTheDocument())
  })
})

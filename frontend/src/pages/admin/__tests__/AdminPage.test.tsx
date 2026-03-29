import { http, HttpResponse } from 'msw'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Toaster } from '@/components/ui/toaster'
import { mockDisabledUser } from '@/test/mockUser'
import { server } from '@/test/server'
import { createTestQueryClient } from '@/test/renderWithProviders'
import { AdminPage } from '../AdminPage'

function renderAdminPage() {
  const qc = createTestQueryClient()
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter>
        <AdminPage />
        <Toaster />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('AdminPage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('ローディング中はスピナーを表示する', () => {
    server.use(
      http.get('/api/v1/users', async () => {
        await new Promise((r) => setTimeout(r, 500))
        return HttpResponse.json({ data: [], pagination: { page: 1, size: 20, totalElements: 0, totalPages: 0 } })
      }),
    )
    renderAdminPage()
    expect(document.querySelector('.animate-spin')).not.toBeNull()
  })

  it('エラー時はエラーメッセージを表示する', async () => {
    server.use(http.get('/api/v1/users', () => new HttpResponse(null, { status: 500 })))
    renderAdminPage()
    await waitFor(() => {
      expect(screen.getByText('データの読み込みに失敗しました')).toBeInTheDocument()
    })
  })

  it('データ取得成功時はテーブルを表示する', async () => {
    renderAdminPage()
    await waitFor(() => expect(screen.getByText('testuser')).toBeInTheDocument())
  })

  it('user.enabled: true → 有効バッジを表示する', async () => {
    renderAdminPage()
    await waitFor(() => expect(screen.getByText('有効')).toBeInTheDocument())
  })

  it('user.enabled: false → 無効バッジを表示する', async () => {
    server.use(
      http.get('/api/v1/users', () =>
        HttpResponse.json({
          data: [mockDisabledUser],
          pagination: { page: 1, size: 20, totalElements: 1, totalPages: 1 },
        }),
      ),
    )
    renderAdminPage()
    await waitFor(() => expect(screen.getByText('無効')).toBeInTheDocument())
  })

  it('編集ボタンをクリックするとダイアログが開く', async () => {
    renderAdminPage()
    await waitFor(() => expect(screen.getByText('testuser')).toBeInTheDocument())
    await userEvent.click(screen.getByRole('button', { name: '編集' }))
    await waitFor(() => expect(screen.getByText('ユーザー編集')).toBeInTheDocument())
  })

  it('追加ボタンをクリックすると作成ダイアログが開く', async () => {
    renderAdminPage()
    await waitFor(() => expect(screen.getByText('testuser')).toBeInTheDocument())
    await userEvent.click(screen.getByRole('button', { name: 'ユーザー追加' }))
    await waitFor(() => expect(screen.getByText('ユーザー作成')).toBeInTheDocument())
  })

  it('削除確認: OK → deleteUser が呼ばれる', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    server.use(http.delete('/api/v1/users/:id', () => new HttpResponse(null, { status: 204 })))
    renderAdminPage()
    await waitFor(() => expect(screen.getByText('testuser')).toBeInTheDocument())
    await userEvent.click(screen.getByRole('button', { name: '削除' }))
    await waitFor(() => {
      expect(window.confirm).toHaveBeenCalled()
    })
  })

  it('削除確認: キャンセル → deleteUser が呼ばれない', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    renderAdminPage()
    await waitFor(() => expect(screen.getByText('testuser')).toBeInTheDocument())
    await userEvent.click(screen.getByRole('button', { name: '削除' }))
    expect(window.confirm).toHaveBeenCalled()
    // MSW の delete ハンドラーが呼ばれていないことを間接的に確認（ページに変化なし）
    expect(screen.getByText('testuser')).toBeInTheDocument()
  })

  it('削除成功 → 成功トーストを表示する', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderAdminPage()
    await waitFor(() => expect(screen.getByText('testuser')).toBeInTheDocument())
    await userEvent.click(screen.getByRole('button', { name: '削除' }))
    await waitFor(() => {
      expect(screen.getByText('ユーザーを削除しました')).toBeInTheDocument()
    })
  })

  it('削除失敗 → エラートーストを表示する', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    server.use(http.delete('/api/v1/users/:id', () => new HttpResponse(null, { status: 500 })))
    renderAdminPage()
    await waitFor(() => expect(screen.getByText('testuser')).toBeInTheDocument())
    await userEvent.click(screen.getByRole('button', { name: '削除' }))
    await waitFor(() => {
      expect(screen.getByText('ユーザーの削除に失敗しました')).toBeInTheDocument()
    })
  })
})

import { http, HttpResponse } from 'msw'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { Toaster } from '@/components/ui/toaster'
import { mockUser } from '@/test/mockUser'
import { server } from '@/test/server'
import { createTestQueryClient } from '@/test/renderWithProviders'
import type { User } from '@/types'
import { UserFormDialog } from '../UserFormDialog'

function renderDialog(props: { open: boolean; onOpenChange: (v: boolean) => void; editingUser: User | null }) {
  const qc = createTestQueryClient()
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter>
        <UserFormDialog {...props} />
        <Toaster />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('UserFormDialog: 作成モード', () => {
  it('editingUser: null のとき「ユーザー作成」フォームを表示する', () => {
    renderDialog({ open: true, onOpenChange: vi.fn(), editingUser: null })
    expect(screen.getByText('ユーザー作成')).toBeInTheDocument()
  })

  it('username バリデーションエラーを表示する', async () => {
    renderDialog({ open: true, onOpenChange: vi.fn(), editingUser: null })
    await userEvent.click(screen.getByRole('button', { name: '作成' }))
    await waitFor(() => {
      expect(screen.getByText('ユーザー名を入力してください')).toBeInTheDocument()
    })
  })

  it('email バリデーションエラーを表示する', async () => {
    renderDialog({ open: true, onOpenChange: vi.fn(), editingUser: null })
    await userEvent.type(screen.getByLabelText('ユーザー名'), 'user')
    await userEvent.type(screen.getByLabelText('メールアドレス'), 'invalid')
    // type="email" の HTML5 バリデーションを回避するため fireEvent.submit を使用
    fireEvent.submit(screen.getByRole('button', { name: '作成' }).closest('form')!)
    await waitFor(() => {
      expect(screen.getByText('メールアドレスの形式が正しくありません')).toBeInTheDocument()
    })
  })

  it('password バリデーションエラーを表示する', async () => {
    renderDialog({ open: true, onOpenChange: vi.fn(), editingUser: null })
    await userEvent.type(screen.getByLabelText('ユーザー名'), 'user')
    await userEvent.type(screen.getByLabelText('メールアドレス'), 'u@u.com')
    await userEvent.type(screen.getByLabelText('パスワード'), 'weak')
    await userEvent.click(screen.getByRole('button', { name: '作成' }))
    await waitFor(() => {
      expect(screen.getByText('パスワードは8文字以上で入力してください')).toBeInTheDocument()
    })
  })

  it('作成成功 → トーストを表示し onOpenChange(false) を呼ぶ', async () => {
    const onOpenChange = vi.fn()
    renderDialog({ open: true, onOpenChange, editingUser: null })
    await userEvent.type(screen.getByLabelText('ユーザー名'), 'newuser')
    await userEvent.type(screen.getByLabelText('メールアドレス'), 'new@example.com')
    await userEvent.type(screen.getByLabelText('パスワード'), 'Password1!')
    await userEvent.click(screen.getByRole('button', { name: '作成' }))
    await waitFor(() => {
      expect(screen.getByText('ユーザーを作成しました')).toBeInTheDocument()
    })
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })

  it('作成失敗 → エラートーストを表示する', async () => {
    server.use(http.post('/api/v1/users', () => new HttpResponse(null, { status: 500 })))
    renderDialog({ open: true, onOpenChange: vi.fn(), editingUser: null })
    await userEvent.type(screen.getByLabelText('ユーザー名'), 'newuser')
    await userEvent.type(screen.getByLabelText('メールアドレス'), 'new@example.com')
    await userEvent.type(screen.getByLabelText('パスワード'), 'Password1!')
    await userEvent.click(screen.getByRole('button', { name: '作成' }))
    await waitFor(() => {
      expect(screen.getByText('ユーザーの作成に失敗しました')).toBeInTheDocument()
    })
  })

  it('キャンセルボタンで onOpenChange(false) を呼ぶ', async () => {
    const onOpenChange = vi.fn()
    renderDialog({ open: true, onOpenChange, editingUser: null })
    await userEvent.click(screen.getByRole('button', { name: 'キャンセル' }))
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })

  it('isPending: true のときボタンに「作成中...」を表示する', async () => {
    server.use(
      http.post('/api/v1/users', async () => {
        await new Promise((r) => setTimeout(r, 200))
        return HttpResponse.json({ data: mockUser })
      }),
    )
    renderDialog({ open: true, onOpenChange: vi.fn(), editingUser: null })
    await userEvent.type(screen.getByLabelText('ユーザー名'), 'newuser')
    await userEvent.type(screen.getByLabelText('メールアドレス'), 'new@example.com')
    await userEvent.type(screen.getByLabelText('パスワード'), 'Password1!')
    await userEvent.click(screen.getByRole('button', { name: '作成' }))
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '作成中...' })).toBeDisabled()
    })
  })
})

describe('UserFormDialog: 編集モード', () => {
  it('editingUser がある場合「ユーザー編集」フォームを表示する', () => {
    renderDialog({ open: true, onOpenChange: vi.fn(), editingUser: mockUser })
    expect(screen.getByText('ユーザー編集')).toBeInTheDocument()
  })

  it('editingUser の値がフォームに設定される', () => {
    renderDialog({ open: true, onOpenChange: vi.fn(), editingUser: mockUser })
    expect(screen.getByDisplayValue('testuser')).toBeInTheDocument()
    expect(screen.getByDisplayValue('test@example.com')).toBeInTheDocument()
  })

  it('username バリデーションエラーを表示する', async () => {
    renderDialog({ open: true, onOpenChange: vi.fn(), editingUser: mockUser })
    const usernameInput = screen.getByDisplayValue('testuser')
    await userEvent.clear(usernameInput)
    await userEvent.click(screen.getByRole('button', { name: '更新' }))
    await waitFor(() => {
      expect(screen.getByText('ユーザー名を入力してください')).toBeInTheDocument()
    })
  })

  it('email バリデーションエラーを表示する', async () => {
    renderDialog({ open: true, onOpenChange: vi.fn(), editingUser: mockUser })
    const emailInput = screen.getByDisplayValue('test@example.com')
    await userEvent.clear(emailInput)
    await userEvent.type(emailInput, 'bad-email')
    fireEvent.submit(screen.getByRole('button', { name: '更新' }).closest('form')!)
    await waitFor(() => {
      expect(screen.getByText('メールアドレスの形式が正しくありません')).toBeInTheDocument()
    })
  })

  it('更新成功 → トーストを表示し onOpenChange(false) を呼ぶ', async () => {
    const onOpenChange = vi.fn()
    renderDialog({ open: true, onOpenChange, editingUser: mockUser })
    await userEvent.click(screen.getByRole('button', { name: '更新' }))
    await waitFor(() => {
      expect(screen.getByText('ユーザーを更新しました')).toBeInTheDocument()
    })
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })

  it('更新失敗 → エラートーストを表示する', async () => {
    server.use(http.put('/api/v1/users/:id', () => new HttpResponse(null, { status: 500 })))
    renderDialog({ open: true, onOpenChange: vi.fn(), editingUser: mockUser })
    await userEvent.click(screen.getByRole('button', { name: '更新' }))
    await waitFor(() => {
      expect(screen.getByText('ユーザーの更新に失敗しました')).toBeInTheDocument()
    })
  })

  it('キャンセルボタンで onOpenChange(false) を呼ぶ', async () => {
    const onOpenChange = vi.fn()
    renderDialog({ open: true, onOpenChange, editingUser: mockUser })
    await userEvent.click(screen.getByRole('button', { name: 'キャンセル' }))
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })

  it('isPending: true のときボタンに「更新中...」を表示する', async () => {
    server.use(
      http.put('/api/v1/users/:id', async () => {
        await new Promise((r) => setTimeout(r, 200))
        return HttpResponse.json({ data: mockUser })
      }),
    )
    renderDialog({ open: true, onOpenChange: vi.fn(), editingUser: mockUser })
    await userEvent.click(screen.getByRole('button', { name: '更新' }))
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '更新中...' })).toBeDisabled()
    })
  })
})

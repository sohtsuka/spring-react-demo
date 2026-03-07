import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import { ForbiddenPage } from '../ForbiddenPage'

describe('ForbiddenPage', () => {
  it('403 テキストとメッセージを表示する', () => {
    render(<MemoryRouter><ForbiddenPage /></MemoryRouter>)
    expect(screen.getByText('403')).toBeInTheDocument()
    expect(screen.getByText('アクセス権限がありません')).toBeInTheDocument()
  })

  it('ダッシュボードへのリンクが存在する', () => {
    render(<MemoryRouter><ForbiddenPage /></MemoryRouter>)
    expect(screen.getByRole('link', { name: 'ダッシュボードへ戻る' })).toBeInTheDocument()
  })
})

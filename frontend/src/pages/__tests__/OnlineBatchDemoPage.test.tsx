import { fireEvent, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { onlineBatchApi } from '@/api/onlineBatch'
import * as toastHook from '@/hooks/useToast'
import { OnlineBatchDemoPage } from '../OnlineBatchDemoPage'
import { renderWithProviders } from '@/test/renderWithProviders'
import { server } from '@/test/server'

describe('OnlineBatchDemoPage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('ジョブがない場合は空状態を表示する', async () => {
    server.use(
      http.get('/api/v1/online-batch-jobs', () => HttpResponse.json({ data: [] })),
    )

    renderWithProviders(<OnlineBatchDemoPage />)

    expect(await screen.findByText('まだジョブはありません')).toBeInTheDocument()
    expect(screen.getByText('ジョブを選択してください')).toBeInTheDocument()
  })

  it('一覧取得エラー時はエラーメッセージを表示する', async () => {
    server.use(
      http.get('/api/v1/online-batch-jobs', () => new HttpResponse(null, { status: 500 })),
    )

    renderWithProviders(<OnlineBatchDemoPage />)

    expect(await screen.findByText('ジョブ一覧の取得に失敗しました')).toBeInTheDocument()
  })

  it('一覧と詳細を表示する', async () => {
    renderWithProviders(<OnlineBatchDemoPage />)

    expect(await screen.findByText('オンラインバッチデモ')).toBeInTheDocument()
    expect(await screen.findByText('売上CSV取込デモ')).toBeInTheDocument()
    expect(await screen.findByText('37%')).toBeInTheDocument()
    expect(await screen.findByText(/現在処理中: データ4\/8/)).toBeInTheDocument()
  })

  it('一覧から別ジョブを選択すると詳細を切り替える', async () => {
    const firstJob = {
      id: 1,
      jobName: '売上CSV取込デモ',
      status: 'RUNNING',
      totalItems: 8,
      processedItems: 3,
      successCount: 3,
      failureCount: 0,
      progressPercent: 37,
      failureAtItem: null,
      processingDelayMs: 400,
      currentItem: 'データ4/8',
      createdAt: '2026-04-25T10:00:00',
      startedAt: '2026-04-25T10:00:01',
      completedAt: null,
      recentEvents: ['2026-04-25T10:00:03 データ3/8 の処理が完了しました'],
    }
    const secondJob = {
      id: 2,
      jobName: '夜間集計デモ',
      status: 'FAILED',
      totalItems: 5,
      processedItems: 2,
      successCount: 1,
      failureCount: 1,
      progressPercent: 40,
      failureAtItem: 2,
      processingDelayMs: 300,
      currentItem: 'データ2/5',
      createdAt: '2026-04-25T11:00:00',
      startedAt: '2026-04-25T11:00:01',
      completedAt: '2026-04-25T11:00:02',
      recentEvents: ['2026-04-25T11:00:02 データ2/5 でエラーが発生し、ジョブを停止しました'],
    }

    server.use(
      http.get('/api/v1/online-batch-jobs', () => HttpResponse.json({ data: [firstJob, secondJob] })),
      http.get('/api/v1/online-batch-jobs/1', () => HttpResponse.json({ data: firstJob })),
      http.get('/api/v1/online-batch-jobs/2', () => HttpResponse.json({ data: secondJob })),
    )

    const user = userEvent.setup()
    renderWithProviders(<OnlineBatchDemoPage />)

    expect(await screen.findByText(/現在処理中: データ4\/8/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /夜間集計デモ/ }))

    expect(await screen.findByText(/現在処理中: データ2\/5/)).toBeInTheDocument()
    expect(screen.getByText('40%')).toBeInTheDocument()
    expect(screen.getAllByText('夜間集計デモ').length).toBeGreaterThan(0)
  })

  it('完了済みジョブは待機または完了と完了バッジを表示する', async () => {
    const completedJob = {
      id: 9,
      jobName: '月次締めデモ',
      status: 'COMPLETED',
      totalItems: 4,
      processedItems: 4,
      successCount: 4,
      failureCount: 0,
      progressPercent: 100,
      failureAtItem: null,
      processingDelayMs: 200,
      currentItem: null,
      createdAt: '2026-04-25T12:00:00',
      startedAt: '2026-04-25T12:00:01',
      completedAt: '2026-04-25T12:00:03',
      recentEvents: ['2026-04-25T12:00:03 すべてのデータを処理しました'],
    }

    server.use(
      http.get('/api/v1/online-batch-jobs', () => HttpResponse.json({ data: [completedJob] })),
      http.get('/api/v1/online-batch-jobs/9', () => HttpResponse.json({ data: completedJob })),
    )

    renderWithProviders(<OnlineBatchDemoPage />)

    expect(await screen.findByText(/現在処理中: 待機または完了/)).toBeInTheDocument()
    expect(screen.getByText('100%')).toBeInTheDocument()
    expect(screen.getAllByText('完了').length).toBeGreaterThan(0)
  })

  it('更新ボタン押下で一覧を再取得できる', async () => {
    let requestCount = 0
    server.use(
      http.get('/api/v1/online-batch-jobs', () => {
        requestCount += 1
        return HttpResponse.json({ data: [] })
      }),
    )

    const user = userEvent.setup()
    renderWithProviders(<OnlineBatchDemoPage />)

    await screen.findByText('まだジョブはありません')
    await user.click(screen.getByRole('button', { name: '更新' }))

    await waitFor(() => {
      expect(requestCount).toBeGreaterThanOrEqual(2)
    })
  })

  it('フォーム送信でジョブを起動する', async () => {
    const startedJob = {
      id: 5,
      jobName: '夜間連携デモ',
      status: 'ACCEPTED' as const,
      totalItems: 12,
      processedItems: 0,
      successCount: 0,
      failureCount: 0,
      progressPercent: 0,
      failureAtItem: null,
      processingDelayMs: 250,
      currentItem: null,
      createdAt: '2026-04-25T10:00:00',
      startedAt: null,
      completedAt: null,
      recentEvents: ['2026-04-25T10:00:00 ジョブを受け付けました'],
    }

    server.use(
      http.get('/api/v1/online-batch-jobs', () => HttpResponse.json({ data: [] })),
      http.get('/api/v1/online-batch-jobs/5', () =>
        HttpResponse.json({ data: startedJob }),
      ),
    )

    const user = userEvent.setup()
    const toast = vi.fn()
    vi.spyOn(toastHook, 'useToast').mockReturnValue({
      toast,
      dismiss: vi.fn(),
      toasts: [],
    })
    const { queryClient } = renderWithProviders(<OnlineBatchDemoPage />)
    const invalidateQueries = vi.spyOn(queryClient, 'invalidateQueries')
    const startJob = vi.spyOn(onlineBatchApi, 'startJob').mockResolvedValue(startedJob)

    expect(await screen.findByText('まだジョブはありません')).toBeInTheDocument()

    const jobNameInput = screen.getByLabelText('ジョブ名')
    await user.clear(jobNameInput)
    await user.type(jobNameInput, '夜間連携デモ')
    await user.clear(screen.getByLabelText('処理件数'))
    await user.type(screen.getByLabelText('処理件数'), '12')
    await user.clear(screen.getByLabelText('処理間隔(ms)'))
    await user.type(screen.getByLabelText('処理間隔(ms)'), '250')
    await user.type(screen.getByLabelText('失敗させる件番'), '3')
    await user.clear(screen.getByLabelText('失敗させる件番'))
    fireEvent.submit(screen.getByRole('button', { name: 'バッチを起動' }).closest('form')!)

    await waitFor(() => {
      expect(startJob).toHaveBeenCalled()
    })
    expect(startJob.mock.calls[0]?.[0]).toEqual({
        jobName: '夜間連携デモ',
        totalItems: 12,
        failureAtItem: null,
        processingDelayMs: 250,
    })
    expect(toast).toHaveBeenCalledWith({
      title: 'オンラインバッチを受け付けました',
      description: '夜間連携デモ をバックグラウンドで実行しています',
    })
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: ['online-batch-jobs'],
    })
    expect(invalidateQueries).toHaveBeenCalledWith({
      queryKey: ['online-batch-job', 5],
    })
  })

  it('ジョブ起動失敗時はエラートーストを表示する', async () => {
    const toast = vi.fn()
    vi.spyOn(toastHook, 'useToast').mockReturnValue({
      toast,
      dismiss: vi.fn(),
      toasts: [],
    })
    vi.spyOn(onlineBatchApi, 'startJob').mockRejectedValue(new Error('failed'))
    renderWithProviders(<OnlineBatchDemoPage />)

    fireEvent.submit((await screen.findByRole('button', { name: 'バッチを起動' })).closest('form')!)

    await waitFor(() => {
      expect(toast).toHaveBeenCalledWith({
        title: 'エラー',
        description: 'オンラインバッチの起動に失敗しました',
        variant: 'destructive',
      })
    })
  })

  it('詳細取得エラー時はエラーメッセージを表示する', async () => {
    server.use(
      http.get('/api/v1/online-batch-jobs/1', () => new HttpResponse(null, { status: 500 })),
    )

    renderWithProviders(<OnlineBatchDemoPage />)

    expect(await screen.findByText('ジョブ詳細の取得に失敗しました')).toBeInTheDocument()
  })
})

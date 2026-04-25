import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { OnlineBatchDemoPage } from '../OnlineBatchDemoPage'
import { renderWithProviders } from '@/test/renderWithProviders'
import { server } from '@/test/server'

describe('OnlineBatchDemoPage', () => {
  it('一覧と詳細を表示する', async () => {
    renderWithProviders(<OnlineBatchDemoPage />)

    expect(await screen.findByText('オンラインバッチデモ')).toBeInTheDocument()
    expect(await screen.findByText('売上CSV取込デモ')).toBeInTheDocument()
    expect(await screen.findByText('37%')).toBeInTheDocument()
    expect(await screen.findByText(/現在処理中: データ4\/8/)).toBeInTheDocument()
  })

  it('フォーム送信でジョブを起動する', async () => {
    const jobs = [
      {
        id: 5,
        jobName: '夜間連携デモ',
        status: 'ACCEPTED',
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
      },
    ]

    server.use(
      http.get('/api/v1/online-batch-jobs', () => HttpResponse.json({ data: jobs })),
      http.post('/api/v1/online-batch-jobs', async ({ request }) => {
        const body = await request.json() as Record<string, unknown>
        jobs[0] = {
          ...jobs[0],
          jobName: String(body.jobName),
          totalItems: Number(body.totalItems),
          processingDelayMs: Number(body.processingDelayMs),
          failureAtItem: body.failureAtItem === null ? null : Number(body.failureAtItem),
        }
        return HttpResponse.json({ data: jobs[0] }, { status: 201 })
      }),
      http.get('/api/v1/online-batch-jobs/5', () => HttpResponse.json({ data: jobs[0] })),
    )

    const user = userEvent.setup()
    renderWithProviders(<OnlineBatchDemoPage />)

    const jobNameInput = await screen.findByLabelText('ジョブ名')
    await user.clear(jobNameInput)
    await user.type(jobNameInput, '夜間連携デモ')
    await user.clear(screen.getByLabelText('処理件数'))
    await user.type(screen.getByLabelText('処理件数'), '12')
    await user.clear(screen.getByLabelText('処理間隔(ms)'))
    await user.type(screen.getByLabelText('処理間隔(ms)'), '250')
    await user.click(screen.getByRole('button', { name: 'バッチを起動' }))

    await waitFor(() => {
      expect(screen.getAllByText('夜間連携デモ').length).toBeGreaterThan(0)
    })
    expect(screen.getByText('0%')).toBeInTheDocument()
  })
})

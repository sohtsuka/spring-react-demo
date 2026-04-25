import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '@/test/server'
import { onlineBatchApi } from '../onlineBatch'

const mockJob = {
  id: 1,
  jobName: '売上CSV取込デモ',
  status: 'RUNNING' as const,
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

const mockListJob = {
  ...mockJob,
  recentEvents: [
    '2026-04-25T10:00:03 データ3/8 の処理が完了しました',
    '2026-04-25T10:00:02 データ3/8 の処理を開始しました',
  ],
}

const mockStartedJob = {
  ...mockJob,
  id: 2,
  jobName: '起動デモ',
  status: 'ACCEPTED' as const,
  totalItems: 6,
  processedItems: 0,
  successCount: 0,
  progressPercent: 0,
  processingDelayMs: 200,
  currentItem: null,
  recentEvents: ['2026-04-25T10:01:00 ジョブを受け付けました'],
}

describe('onlineBatchApi', () => {
  it('getJobs: ジョブ一覧を返す', async () => {
    const result = await onlineBatchApi.getJobs()
    expect(result).toEqual([mockListJob])
  })

  it('getJob: 単一ジョブを返す', async () => {
    const result = await onlineBatchApi.getJob(1)
    expect(result).toEqual(mockListJob)
  })

  it('startJob: 起動したジョブを返す', async () => {
    const result = await onlineBatchApi.startJob({
      jobName: '起動デモ',
      totalItems: 6,
      failureAtItem: null,
      processingDelayMs: 200,
    })
    expect(result).toEqual(mockStartedJob)
  })

  it('getJobs: エラー時は reject される', async () => {
    server.use(
      http.get('/api/v1/online-batch-jobs', () => new HttpResponse(null, { status: 500 })),
    )
    await expect(onlineBatchApi.getJobs()).rejects.toThrow()
  })
})

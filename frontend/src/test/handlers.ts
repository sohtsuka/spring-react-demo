import { http, HttpResponse } from 'msw'
import { mockAuthUser, mockUser } from './mockUser'

const runningBatchJob = {
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
  recentEvents: [
    '2026-04-25T10:00:03 データ3/8 の処理が完了しました',
    '2026-04-25T10:00:02 データ3/8 の処理を開始しました',
  ],
}

export const handlers = [
  http.get('/api/v1/auth/me', () => HttpResponse.json({ data: mockAuthUser })),
  http.post('/api/v1/auth/login', () => HttpResponse.json({ data: mockAuthUser })),
  http.post('/api/v1/auth/logout', () => new HttpResponse(null, { status: 200 })),
  http.get('/api/v1/users', () =>
    HttpResponse.json({
      data: [mockUser],
      pagination: { page: 1, size: 20, totalElements: 1, totalPages: 1 },
    }),
  ),
  http.get('/api/v1/users/:id', () => HttpResponse.json({ data: mockUser })),
  http.post('/api/v1/users', () => HttpResponse.json({ data: mockUser }, { status: 201 })),
  http.put('/api/v1/users/:id', () => HttpResponse.json({ data: mockUser })),
  http.delete('/api/v1/users/:id', () => new HttpResponse(null, { status: 204 })),
  http.get('/api/v1/online-batch-jobs', () => HttpResponse.json({ data: [runningBatchJob] })),
  http.get('/api/v1/online-batch-jobs/:id', () => HttpResponse.json({ data: runningBatchJob })),
  http.post('/api/v1/online-batch-jobs', async ({ request }) => {
    const body = await request.json() as Record<string, unknown>
    return HttpResponse.json({
      data: {
        ...runningBatchJob,
        id: 2,
        jobName: body.jobName ?? '新規ジョブ',
        totalItems: body.totalItems ?? 8,
        failureAtItem: body.failureAtItem ?? null,
        processingDelayMs: body.processingDelayMs ?? 400,
        status: 'ACCEPTED',
        processedItems: 0,
        successCount: 0,
        failureCount: 0,
        progressPercent: 0,
        currentItem: null,
        recentEvents: ['2026-04-25T10:01:00 ジョブを受け付けました'],
      },
    }, { status: 201 })
  }),
]

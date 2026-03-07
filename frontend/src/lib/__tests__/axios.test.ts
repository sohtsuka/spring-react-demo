import { http, HttpResponse } from 'msw'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { server } from '@/test/server'
import api from '../axios'

describe('getCookieValue (via CSRF interceptor)', () => {
  afterEach(() => {
    // Reset cookie after each test
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT'
  })

  it('クッキーが存在する場合は CSRF トークンをヘッダーに付与する', async () => {
    let receivedCsrfHeader: string | null = null
    server.use(
      http.post('/api/v1/auth/login', ({ request }) => {
        receivedCsrfHeader = request.headers.get('X-CSRF-TOKEN')
        return HttpResponse.json({ data: {} })
      }),
    )
    document.cookie = 'XSRF-TOKEN=test-csrf-token'
    await api.post('/auth/login', {})
    expect(receivedCsrfHeader).toBe('test-csrf-token')
  })

  it('クッキーが存在しない場合は CSRF トークンヘッダーを付与しない', async () => {
    let receivedCsrfHeader: string | null = null
    server.use(
      http.post('/api/v1/auth/login', ({ request }) => {
        receivedCsrfHeader = request.headers.get('X-CSRF-TOKEN')
        return HttpResponse.json({ data: {} })
      }),
    )
    // No cookie set
    await api.post('/auth/login', {})
    expect(receivedCsrfHeader).toBeNull()
  })

  it('GET リクエストは CSRF トークンヘッダーを付与しない', async () => {
    let receivedCsrfHeader: string | null = null
    server.use(
      http.get('/api/v1/auth/me', ({ request }) => {
        receivedCsrfHeader = request.headers.get('X-CSRF-TOKEN')
        return HttpResponse.json({ data: {} })
      }),
    )
    document.cookie = 'XSRF-TOKEN=test-csrf-token'
    await api.get('/auth/me')
    expect(receivedCsrfHeader).toBeNull()
  })
})

describe('レスポンスインターセプター', () => {
  it('成功レスポンスはそのまま返す', async () => {
    server.use(
      http.get('/api/v1/auth/me', () => HttpResponse.json({ data: { id: 1 } })),
    )
    const response = await api.get('/auth/me')
    expect(response.status).toBe(200)
    expect(response.data).toEqual({ data: { id: 1 } })
  })

  it('エラーレスポンスは reject される', async () => {
    server.use(
      http.get('/api/v1/auth/me', () => new HttpResponse(null, { status: 500 })),
    )
    await expect(api.get('/auth/me')).rejects.toThrow()
  })
})

describe('CSRF インターセプター: 非安全メソッド', () => {
  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=my-token'
  })
  afterEach(() => {
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT'
  })

  it.each(['put', 'delete', 'patch'] as const)(
    '%s リクエストも CSRF トークンを付与する',
    async (method) => {
      let receivedCsrfHeader: string | null = null
      server.use(
        http[method]('/api/v1/users/1', ({ request }) => {
          receivedCsrfHeader = request.headers.get('X-CSRF-TOKEN')
          return new HttpResponse(null, { status: 200 })
        }),
      )
      await api[method]('/users/1', {}).catch(() => {})
      expect(receivedCsrfHeader).toBe('my-token')
    },
  )
})

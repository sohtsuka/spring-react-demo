import { http, HttpResponse } from 'msw'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { server } from '@/test/server'
import api, { HttpError } from '../api'

describe('getCookieValue (via CSRF インターセプター)', () => {
  afterEach(() => {
    // Reset cookie after each test
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT'
  })

  it('クッキーが存在する場合は CSRF トークンをヘッダーに付与する', async () => {
    let receivedCsrfHeader: string | null = null
    server.use(
      http.post('/api/v1/auth/login', ({ request }) => {
        receivedCsrfHeader = request.headers.get('X-XSRF-TOKEN')
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
        receivedCsrfHeader = request.headers.get('X-XSRF-TOKEN')
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
        receivedCsrfHeader = request.headers.get('X-XSRF-TOKEN')
        return HttpResponse.json({ data: {} })
      }),
    )
    document.cookie = 'XSRF-TOKEN=test-csrf-token'
    await api.get('/auth/me')
    expect(receivedCsrfHeader).toBeNull()
  })
})

describe('クエリパラメータ', () => {
  it('undefined の値はクエリ文字列に含めない', async () => {
    let receivedUrl: string | null = null
    server.use(
      http.get('/api/v1/users', ({ request }) => {
        receivedUrl = request.url
        return HttpResponse.json({ data: [] })
      }),
    )
    await api.get('/users', { params: { page: 1, size: undefined } })
    expect(receivedUrl).toContain('page=1')
    expect(receivedUrl).not.toContain('size')
  })
})

describe('レスポンス処理', () => {
  it('成功レスポンスは JSON を返す', async () => {
    server.use(
      http.get('/api/v1/auth/me', () => HttpResponse.json({ data: { id: 1 } })),
    )
    const data = await api.get('/auth/me')
    expect(data).toEqual({ data: { id: 1 } })
  })

  it('エラーレスポンスは HttpError を throw する', async () => {
    server.use(
      http.get('/api/v1/auth/me', () => new HttpResponse(null, { status: 500 })),
    )
    await expect(api.get('/auth/me')).rejects.toBeInstanceOf(HttpError)
  })

  it('HttpError にはステータスコードが含まれる', async () => {
    server.use(
      http.get('/api/v1/auth/me', () => new HttpResponse(null, { status: 404 })),
    )
    const error = await api.get('/auth/me').catch((e) => e)
    expect(error).toBeInstanceOf(HttpError)
    expect((error as HttpError).status).toBe(404)
  })
})

describe('CSRF インターセプター: 非安全メソッド', () => {
  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=my-token'
  })
  afterEach(() => {
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT'
  })

  it.each(['put', 'patch'] as const)(
    '%s リクエストも CSRF トークンを付与する',
    async (method) => {
      let receivedCsrfHeader: string | null = null
      server.use(
        http[method]('/api/v1/users/1', ({ request }) => {
          receivedCsrfHeader = request.headers.get('X-XSRF-TOKEN')
          return new HttpResponse(null, { status: 200 })
        }),
      )
      await api[method]('/users/1', {}).catch(() => {})
      expect(receivedCsrfHeader).toBe('my-token')
    },
  )

  it('delete リクエストも CSRF トークンを付与する', async () => {
    let receivedCsrfHeader: string | null = null
    server.use(
      http.delete('/api/v1/users/1', ({ request }) => {
        receivedCsrfHeader = request.headers.get('X-XSRF-TOKEN')
        return new HttpResponse(null, { status: 204 })
      }),
    )
    await api.delete('/users/1').catch(() => {})
    expect(receivedCsrfHeader).toBe('my-token')
  })
})

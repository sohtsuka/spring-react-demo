import { AxiosError, AxiosHeaders } from 'axios'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { queryClient } from '../queryClient'

// QueryCache の onError コールバックを直接取得して呼び出す
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const getOnError = (): ((error: unknown) => void) => (queryClient.getQueryCache() as any).config.onError

function makeAxiosError(status: number): AxiosError {
  return new AxiosError(
    'Request failed',
    String(status),
    undefined,
    undefined,
    { status, data: {}, headers: new AxiosHeaders(), config: { headers: new AxiosHeaders() }, statusText: '' } as never,
  )
}

let href = ''
let pathname = '/dashboard'

beforeEach(() => {
  href = ''
  pathname = '/dashboard'
  Object.defineProperty(window, 'location', {
    configurable: true,
    get: () => ({
      get pathname() { return pathname },
      set href(v: string) { href = v },
      get href() { return href },
    }),
  })
})

afterEach(() => {
  queryClient.clear()
  vi.restoreAllMocks()
})

describe('queryClient onError', () => {
  it('Axios エラー + 401 + /login 以外のパス → /login にリダイレクト', () => {
    pathname = '/dashboard'
    getOnError()(makeAxiosError(401))
    expect(href).toBe('/login')
  })

  it('Axios エラー + 401 + /login パス → リダイレクトしない', () => {
    pathname = '/login'
    getOnError()(makeAxiosError(401))
    expect(href).toBe('')
  })

  it('Axios エラー + 非 401 (500) → リダイレクトしない', () => {
    getOnError()(makeAxiosError(500))
    expect(href).toBe('')
  })

  it('非 Axios エラー → リダイレクトしない', () => {
    getOnError()(new Error('plain error'))
    expect(href).toBe('')
  })
})

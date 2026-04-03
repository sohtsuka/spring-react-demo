import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { HttpError } from '../api'
import { queryClient } from '../queryClient'

// QueryCache の onError コールバックを直接取得して呼び出す
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const getOnError = (): ((error: unknown) => void) => (queryClient.getQueryCache() as any).config.onError

function makeHttpError(status: number): HttpError {
  return new HttpError(status, 'Error')
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
  it('HttpError + 401 + /login 以外のパス → /login にリダイレクト', () => {
    pathname = '/dashboard'
    getOnError()(makeHttpError(401))
    expect(href).toBe('/login')
  })

  it('HttpError + 401 + /login パス → リダイレクトしない', () => {
    pathname = '/login'
    getOnError()(makeHttpError(401))
    expect(href).toBe('')
  })

  it('HttpError + 非 401 (500) → リダイレクトしない', () => {
    getOnError()(makeHttpError(500))
    expect(href).toBe('')
  })

  it('非 HttpError → リダイレクトしない', () => {
    getOnError()(new Error('plain error'))
    expect(href).toBe('')
  })
})

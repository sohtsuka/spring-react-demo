import { http, HttpResponse } from 'msw'
import { act, renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router'
import React from 'react'
import { describe, expect, it } from 'vitest'
import { mockAuthUser } from '@/test/mockUser'
import { server } from '@/test/server'
import { useAuth } from '../useAuth'

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return React.createElement(
    QueryClientProvider,
    { client: qc },
    React.createElement(MemoryRouter, null, children),
  )
}

describe('useAuth', () => {
  it('getMe 成功時: user が返り isAuthenticated が true', async () => {
    const { result } = renderHook(() => useAuth(), { wrapper })
    await waitFor(() => expect(result.current.isLoading).toBe(false))
    expect(result.current.user).toEqual(mockAuthUser)
    expect(result.current.isAuthenticated).toBe(true)
  })

  it('getMe が 401 の場合: isAuthenticated が false', async () => {
    server.use(
      http.get('/api/v1/auth/me', () => new HttpResponse(null, { status: 401 })),
    )
    const { result } = renderHook(() => useAuth(), { wrapper })
    await waitFor(() => expect(result.current.isLoading).toBe(false))
    expect(result.current.user).toBeUndefined()
    expect(result.current.isAuthenticated).toBe(false)
  })

  it('login 成功: queryData をセットする', async () => {
    const { result } = renderHook(() => useAuth(), { wrapper })
    await waitFor(() => expect(result.current.isLoading).toBe(false))

    await act(async () => {
      await result.current.loginAsync({ username: 'user', password: 'pass' })
    })

    expect(result.current.user).toEqual(mockAuthUser)
  })

  it('logout 成功: queryClient.clear() が呼ばれてユーザーキャッシュが消える', async () => {
    // getMe は最初成功、logout 後は 401 を返す
    server.use(
      http.post('/api/v1/auth/logout', () => new HttpResponse(null, { status: 200 })),
    )
    const { result } = renderHook(() => useAuth(), { wrapper })
    await waitFor(() => expect(result.current.isLoading).toBe(false))
    expect(result.current.user).toEqual(mockAuthUser)

    // logout 後は getMe が 401 を返すようにセット
    server.use(
      http.get('/api/v1/auth/me', () => new HttpResponse(null, { status: 401 })),
    )
    await act(async () => {
      result.current.logout()
    })
    await waitFor(() => expect(result.current.isAuthenticated).toBe(false))
  })

  it('isLoggingIn: login ミューテーション pending 時に true', async () => {
    let resolveLogin: (() => void) | undefined
    server.use(
      http.post('/api/v1/auth/login', () =>
        new Promise<Response>((resolve) => {
          resolveLogin = () => resolve(HttpResponse.json({ data: mockAuthUser }) as unknown as Response)
        }),
      ),
    )
    const { result } = renderHook(() => useAuth(), { wrapper })
    await waitFor(() => expect(result.current.isLoading).toBe(false))

    act(() => {
      result.current.login({ username: 'u', password: 'p' })
    })
    await waitFor(() => expect(result.current.isLoggingIn).toBe(true))
    resolveLogin?.()
    await waitFor(() => expect(result.current.isLoggingIn).toBe(false))
  })
})

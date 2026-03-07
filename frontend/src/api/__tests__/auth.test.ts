import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { mockAuthUser } from '@/test/mockUser'
import { server } from '@/test/server'
import { authApi } from '../auth'

describe('authApi', () => {
  it('login: 成功レスポンスから data を返す', async () => {
    const result = await authApi.login({ username: 'user', password: 'pass' })
    expect(result).toEqual(mockAuthUser)
  })

  it('logout: 成功時に void を返す', async () => {
    await expect(authApi.logout()).resolves.toBeUndefined()
  })

  it('getMe: 成功レスポンスから data を返す', async () => {
    const result = await authApi.getMe()
    expect(result).toEqual(mockAuthUser)
  })

  it('login: エラー時は reject される', async () => {
    server.use(
      http.post('/api/v1/auth/login', () => new HttpResponse(null, { status: 401 })),
    )
    await expect(authApi.login({ username: 'x', password: 'y' })).rejects.toThrow()
  })
})

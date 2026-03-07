import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { mockUser } from '@/test/mockUser'
import { server } from '@/test/server'
import { usersApi } from '../users'

describe('usersApi', () => {
  it('getUsers: パラメータありで data を返す', async () => {
    const result = await usersApi.getUsers({ page: 1, size: 20 })
    expect(result.data).toEqual([mockUser])
    expect(result.pagination.totalElements).toBe(1)
  })

  it('getUsers: パラメータなし（デフォルト {}）でも動作する', async () => {
    const result = await usersApi.getUsers()
    expect(result.data).toEqual([mockUser])
  })

  it('getUser: 単一ユーザーを返す', async () => {
    const result = await usersApi.getUser(1)
    expect(result).toEqual(mockUser)
  })

  it('createUser: 作成したユーザーを返す', async () => {
    const result = await usersApi.createUser({
      username: 'new',
      email: 'new@example.com',
      password: 'Password1!',
      role: 'USER',
    })
    expect(result).toEqual(mockUser)
  })

  it('updateUser: 更新したユーザーを返す', async () => {
    const result = await usersApi.updateUser(1, {
      username: 'updated',
      email: 'updated@example.com',
      role: 'USER',
      enabled: true,
    })
    expect(result).toEqual(mockUser)
  })

  it('deleteUser: void を返す', async () => {
    await expect(usersApi.deleteUser(1)).resolves.toBeUndefined()
  })

  it('getUsers: エラー時は reject される', async () => {
    server.use(
      http.get('/api/v1/users', () => new HttpResponse(null, { status: 500 })),
    )
    await expect(usersApi.getUsers()).rejects.toThrow()
  })
})

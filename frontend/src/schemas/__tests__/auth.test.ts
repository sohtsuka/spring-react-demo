import { describe, expect, it } from 'vitest'
import { loginSchema } from '../auth'

describe('loginSchema', () => {
  it('有効な入力を受け入れる', () => {
    const result = loginSchema.safeParse({
      username: 'testuser',
      password: 'password123',
    })
    expect(result.success).toBe(true)
  })

  it('ユーザー名が空の場合はエラー', () => {
    const result = loginSchema.safeParse({
      username: '',
      password: 'password123',
    })
    expect(result.success).toBe(false)
    if (!result.success) {
      expect(result.error.issues[0]?.path).toContain('username')
    }
  })

  it('パスワードが8文字未満の場合はエラー', () => {
    const result = loginSchema.safeParse({
      username: 'testuser',
      password: 'short',
    })
    expect(result.success).toBe(false)
    if (!result.success) {
      expect(result.error.issues[0]?.path).toContain('password')
    }
  })

  it('パスワードが空の場合はエラー', () => {
    const result = loginSchema.safeParse({
      username: 'testuser',
      password: '',
    })
    expect(result.success).toBe(false)
  })

  it('ユーザー名が 51 文字以上の場合はエラー', () => {
    const result = loginSchema.safeParse({
      username: 'a'.repeat(51),
      password: 'password123',
    })
    expect(result.success).toBe(false)
    if (!result.success) {
      expect(result.error.issues[0]?.path).toContain('username')
    }
  })
})

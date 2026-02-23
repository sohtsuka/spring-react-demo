import { describe, expect, it } from 'vitest'
import { createUserSchema, updateUserSchema } from '../user'

describe('createUserSchema', () => {
  const validInput = {
    username: 'testuser',
    email: 'test@example.com',
    password: 'Password1!',
    role: 'USER' as const,
  }

  it('有効な入力を受け入れる', () => {
    const result = createUserSchema.safeParse(validInput)
    expect(result.success).toBe(true)
  })

  it('無効なメールアドレスはエラー', () => {
    const result = createUserSchema.safeParse({
      ...validInput,
      email: 'invalid-email',
    })
    expect(result.success).toBe(false)
    if (!result.success) {
      expect(result.error.issues[0]?.path).toContain('email')
    }
  })

  it('大文字を含まないパスワードはエラー', () => {
    const result = createUserSchema.safeParse({
      ...validInput,
      password: 'password1!',
    })
    expect(result.success).toBe(false)
  })

  it('数字を含まないパスワードはエラー', () => {
    const result = createUserSchema.safeParse({
      ...validInput,
      password: 'Password!',
    })
    expect(result.success).toBe(false)
  })

  it('記号を含まないパスワードはエラー', () => {
    const result = createUserSchema.safeParse({
      ...validInput,
      password: 'Password1',
    })
    expect(result.success).toBe(false)
  })

  it('無効なロールはエラー', () => {
    const result = createUserSchema.safeParse({
      ...validInput,
      role: 'SUPERUSER',
    })
    expect(result.success).toBe(false)
  })
})

describe('updateUserSchema', () => {
  const validInput = {
    username: 'testuser',
    email: 'test@example.com',
    role: 'USER' as const,
    enabled: true,
  }

  it('有効な入力を受け入れる', () => {
    const result = updateUserSchema.safeParse(validInput)
    expect(result.success).toBe(true)
  })

  it('enabled が false でも有効', () => {
    const result = updateUserSchema.safeParse({ ...validInput, enabled: false })
    expect(result.success).toBe(true)
  })
})

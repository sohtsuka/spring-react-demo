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

  it('username が空の場合はエラー', () => {
    const result = createUserSchema.safeParse({ ...validInput, username: '' })
    expect(result.success).toBe(false)
  })

  it('username が 51 文字超の場合はエラー', () => {
    const result = createUserSchema.safeParse({ ...validInput, username: 'a'.repeat(51) })
    expect(result.success).toBe(false)
  })

  it('email が空の場合はエラー', () => {
    const result = createUserSchema.safeParse({ ...validInput, email: '' })
    expect(result.success).toBe(false)
  })

  it('email が 256 文字超の場合はエラー', () => {
    // 250 + '@b.com'(6) = 256文字 → 255文字制限を超える
    const result = createUserSchema.safeParse({ ...validInput, email: `${'a'.repeat(250)}@b.com` })
    expect(result.success).toBe(false)
  })

  it('パスワードが 8 文字未満の場合はエラー', () => {
    const result = createUserSchema.safeParse({ ...validInput, password: 'Ab1!' })
    expect(result.success).toBe(false)
  })

  it('英小文字を含まないパスワードはエラー', () => {
    const result = createUserSchema.safeParse({ ...validInput, password: 'PASSWORD1!' })
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

  it('username が空の場合はエラー', () => {
    const result = updateUserSchema.safeParse({ ...validInput, username: '' })
    expect(result.success).toBe(false)
  })

  it('無効なメールアドレスはエラー', () => {
    const result = updateUserSchema.safeParse({ ...validInput, email: 'not-an-email' })
    expect(result.success).toBe(false)
  })
})

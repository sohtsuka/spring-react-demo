import { describe, expect, it } from 'vitest'
import { cn } from '../utils'

describe('cn', () => {
  it('クラス名をマージする', () => {
    expect(cn('foo', 'bar')).toBe('foo bar')
  })

  it('Tailwind の競合クラスを解決する', () => {
    expect(cn('p-4', 'p-8')).toBe('p-8')
  })

  it('falsy 値を無視する', () => {
    expect(cn('foo', false && 'bar', undefined, null, 'baz')).toBe('foo baz')
  })

  it('条件付きクラスを扱える', () => {
    const isActive = true
    expect(cn('base', isActive && 'active')).toBe('base active')
  })
})

import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { reducer, toast, useToast } from '../useToast'

// useToast はモジュールレベルのグローバル状態を持つため、
// テストごとに全トーストを DISMISS してから REMOVE する
afterEach(() => {
  const { result, unmount } = renderHook(() => useToast())
  act(() => {
    result.current.dismiss()
  })
  unmount()
})

describe('reducer', () => {
  const baseState = { toasts: [] }
  const sampleToast = { id: '1', title: 'Test', open: true }

  it('ADD_TOAST: トーストを追加する', () => {
    const next = reducer(baseState, { type: 'ADD_TOAST', toast: sampleToast })
    expect(next.toasts).toHaveLength(1)
    expect(next.toasts[0]).toEqual(sampleToast)
  })

  it('ADD_TOAST: TOAST_LIMIT (1) を超えると切り詰める', () => {
    const state = { toasts: [sampleToast] }
    const next = reducer(state, {
      type: 'ADD_TOAST',
      toast: { id: '2', title: 'Second', open: true },
    })
    expect(next.toasts).toHaveLength(1)
    expect(next.toasts[0].id).toBe('2')
  })

  it('UPDATE_TOAST: 一致する ID を更新する', () => {
    const state = { toasts: [sampleToast] }
    const next = reducer(state, {
      type: 'UPDATE_TOAST',
      toast: { id: '1', title: 'Updated' },
    })
    expect(next.toasts[0].title).toBe('Updated')
  })

  it('UPDATE_TOAST: 不一致の ID は変更しない', () => {
    const state = { toasts: [sampleToast] }
    const next = reducer(state, {
      type: 'UPDATE_TOAST',
      toast: { id: '999', title: 'Other' },
    })
    expect(next.toasts[0].title).toBe('Test')
  })

  it('DISMISS_TOAST: toastId 指定で該当のみ open: false', () => {
    const state = { toasts: [sampleToast] }
    const next = reducer(state, { type: 'DISMISS_TOAST', toastId: '1' })
    expect(next.toasts[0].open).toBe(false)
  })

  it('DISMISS_TOAST: toastId なしで全トーストを open: false', () => {
    const state = { toasts: [sampleToast, { id: '2', title: 'T2', open: true }] }
    const next = reducer(state, { type: 'DISMISS_TOAST', toastId: undefined })
    expect(next.toasts.every((t) => t.open === false)).toBe(true)
  })

  it('DISMISS_TOAST: toastId 指定で一致しない他のトーストは変更しない', () => {
    const state = { toasts: [sampleToast, { id: '2', title: 'T2', open: true }] }
    const next = reducer(state, { type: 'DISMISS_TOAST', toastId: '1' })
    expect(next.toasts[0].open).toBe(false)
    expect(next.toasts[1].open).toBe(true)
  })

  it('REMOVE_TOAST: toastId 指定でそのトーストを削除', () => {
    const state = { toasts: [sampleToast] }
    const next = reducer(state, { type: 'REMOVE_TOAST', toastId: '1' })
    expect(next.toasts).toHaveLength(0)
  })

  it('REMOVE_TOAST: toastId なしで全トーストを削除', () => {
    const state = { toasts: [sampleToast, { id: '2', title: 'T2', open: true }] }
    const next = reducer(state, { type: 'REMOVE_TOAST', toastId: undefined })
    expect(next.toasts).toHaveLength(0)
  })
})

describe('toast()', () => {
  it('onOpenChange(false) → dismiss が呼ばれ open が false になる', () => {
    const { result } = renderHook(() => useToast())
    act(() => {
      toast({ title: 'Hello' })
    })
    expect(result.current.toasts[0].open).toBe(true)
    act(() => {
      result.current.toasts[0].onOpenChange?.(false)
    })
    expect(result.current.toasts[0].open).toBe(false)
  })

  it('onOpenChange(true) → open 状態は変わらない', () => {
    const { result } = renderHook(() => useToast())
    act(() => {
      toast({ title: 'Hello' })
    })
    act(() => {
      result.current.toasts[0].onOpenChange?.(true)
    })
    expect(result.current.toasts[0].open).toBe(true)
  })

  it('update() でトーストを更新できる', () => {
    const { result } = renderHook(() => useToast())
    let toastRef: ReturnType<typeof toast>
    act(() => {
      toastRef = toast({ title: 'Original' })
    })
    act(() => {
      toastRef.update({ id: toastRef.id, title: 'Updated' })
    })
    expect(result.current.toasts[0].title).toBe('Updated')
  })

  it('dismiss() でトーストを閉じられる', () => {
    const { result } = renderHook(() => useToast())
    let toastRef: ReturnType<typeof toast>
    act(() => {
      toastRef = toast({ title: 'ToClose' })
    })
    act(() => {
      toastRef.dismiss()
    })
    expect(result.current.toasts[0].open).toBe(false)
  })
})

describe('addToRemoveQueue', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it('タイムアウト後にトーストが REMOVE される', () => {
    const { result } = renderHook(() => useToast())
    act(() => {
      toast({ title: 'Timeout' })
    })
    act(() => {
      result.current.toasts[0].onOpenChange?.(false) // triggers addToRemoveQueue
    })
    expect(result.current.toasts).toHaveLength(1) // still present
    act(() => {
      vi.runAllTimers()
    })
    expect(result.current.toasts).toHaveLength(0) // removed after timeout
  })

  it('同一 ID を二重に dismiss しても二重登録しない', () => {
    const { result } = renderHook(() => useToast())
    act(() => {
      toast({ title: 'Double' })
    })
    act(() => {
      result.current.toasts[0].onOpenChange?.(false)
      result.current.toasts[0].onOpenChange?.(false) // 2nd call, already in queue
    })
    // Should not throw and should remove exactly once
    act(() => {
      vi.runAllTimers()
    })
    expect(result.current.toasts).toHaveLength(0)
  })
})

describe('useToast()', () => {
  it('リスナーが登録され、toast() 呼び出しで state が更新される', () => {
    const { result } = renderHook(() => useToast())
    act(() => {
      toast({ title: 'Listener Test' })
    })
    expect(result.current.toasts[0].title).toBe('Listener Test')
  })

  it('アンマウント時にリスナーが削除される', () => {
    const { result, unmount } = renderHook(() => useToast())
    act(() => {
      toast({ title: 'Before unmount' })
    })
    unmount()
    // After unmount, dispatching should not cause errors (listener removed)
    act(() => {
      toast({ title: 'After unmount' })
    })
    // result.current is from before unmount, so length is still 1 (original)
    expect(result.current.toasts).toHaveLength(1)
  })

  it('dismiss(toastId) で指定トーストを閉じる', () => {
    const { result } = renderHook(() => useToast())
    act(() => {
      toast({ title: 'Dismiss by id' })
    })
    const id = result.current.toasts[0].id
    act(() => {
      result.current.dismiss(id)
    })
    expect(result.current.toasts[0].open).toBe(false)
  })

  it('dismiss() 引数なしで全トーストを閉じる', () => {
    const { result } = renderHook(() => useToast())
    act(() => {
      toast({ title: 'All dismiss' })
    })
    act(() => {
      result.current.dismiss()
    })
    expect(result.current.toasts.every((t) => t.open === false)).toBe(true)
  })
})

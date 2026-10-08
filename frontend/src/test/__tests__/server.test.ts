import { afterEach, describe, expect, it, vi } from 'vitest'

afterEach(() => vi.restoreAllMocks())

describe('MSW server configuration', () => {
  it('rejects unhandled requests instead of allowing a network passthrough', async () => {
    const error = vi.spyOn(console, 'error').mockImplementation(() => undefined)

    await expect(fetch('http://localhost/api/unhandled-msw-contract')).rejects.toThrow()

    expect(error).toHaveBeenCalled()
  })
})

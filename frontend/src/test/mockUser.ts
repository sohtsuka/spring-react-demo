import type { AuthUser, User } from '@/types'

export const mockAuthUser: AuthUser = {
  id: 1,
  username: 'testuser',
  email: 'test@example.com',
  role: 'USER',
}

export const mockAdminUser: AuthUser = { ...mockAuthUser, role: 'ADMIN' }
export const mockManagerUser: AuthUser = { ...mockAuthUser, role: 'MANAGER' }

export const mockUser: User = {
  ...mockAuthUser,
  enabled: true,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
}

export const mockDisabledUser: User = { ...mockUser, id: 2, enabled: false }

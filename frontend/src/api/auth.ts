import api from '@/lib/axios'
import type { ApiResponse, AuthUser, LoginRequest } from '@/types'

export const authApi = {
  login: async (request: LoginRequest): Promise<AuthUser> => {
    const response = await api.post<ApiResponse<AuthUser>>(
      '/auth/login',
      request,
    )
    return response.data.data
  },

  logout: async (): Promise<void> => {
    await api.post('/auth/logout')
  },

  getMe: async (): Promise<AuthUser> => {
    const response = await api.get<ApiResponse<AuthUser>>('/auth/me')
    return response.data.data
  },
}

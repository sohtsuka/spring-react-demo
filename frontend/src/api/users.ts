import api from '@/lib/axios'
import type {
  ApiResponse,
  CreateUserRequest,
  PaginatedResponse,
  UpdateUserRequest,
  User,
} from '@/types'

export interface GetUsersParams {
  page?: number
  size?: number
}

export const usersApi = {
  getUsers: async (params: GetUsersParams = {}): Promise<PaginatedResponse<User>> => {
    const response = await api.get<PaginatedResponse<User>>('/users', { params })
    return response.data
  },

  getUser: async (id: number): Promise<User> => {
    const response = await api.get<ApiResponse<User>>(`/users/${id}`)
    return response.data.data
  },

  createUser: async (request: CreateUserRequest): Promise<User> => {
    const response = await api.post<ApiResponse<User>>('/users', request)
    return response.data.data
  },

  updateUser: async (id: number, request: UpdateUserRequest): Promise<User> => {
    const response = await api.put<ApiResponse<User>>(`/users/${id}`, request)
    return response.data.data
  },

  deleteUser: async (id: number): Promise<void> => {
    await api.delete(`/users/${id}`)
  },
}

import api from '@/lib/api'
import type { ApiResponse, PaginatedResponse, OnlineBatchJob, StartOnlineBatchRequest } from '@/types'

export const onlineBatchApi = {
  getJobs: async (page = 1): Promise<PaginatedResponse<OnlineBatchJob>> => {
    return api.get<PaginatedResponse<OnlineBatchJob>>('/online-batch-jobs', { params: { page, size: 20 } })
  },

  getJob: async (id: number): Promise<OnlineBatchJob> => {
    const response = await api.get<ApiResponse<OnlineBatchJob>>(`/online-batch-jobs/${id}`)
    return response.data
  },

  startJob: async (request: StartOnlineBatchRequest): Promise<OnlineBatchJob> => {
    const response = await api.post<ApiResponse<OnlineBatchJob>>('/online-batch-jobs', request)
    return response.data
  },
}

import api from '@/lib/api'
import type { ApiResponse, OnlineBatchJob, StartOnlineBatchRequest } from '@/types'

export const onlineBatchApi = {
  getJobs: async (): Promise<OnlineBatchJob[]> => {
    const response = await api.get<ApiResponse<OnlineBatchJob[]>>('/online-batch-jobs')
    return response.data
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

import axios from 'axios'
import { QueryCache, QueryClient } from '@tanstack/react-query'

/**
 * TanStack Query のグローバル設定
 * 401 レスポンス時に /login へリダイレクトする
 */
export const queryClient = new QueryClient({
  queryCache: new QueryCache({
    onError: (error) => {
      if (
        axios.isAxiosError(error) &&
        error.response?.status === 401 &&
        window.location.pathname !== '/login'
      ) {
        window.location.href = '/login'
      }
    },
  }),
  defaultOptions: {
    queries: {
      retry: false,
      staleTime: 1000 * 60 * 5, // 5 分
    },
  },
})

import { QueryCache, QueryClient } from '@tanstack/react-query'
import { HttpError } from '@/lib/api'

/**
 * TanStack Query のグローバル設定
 * 401 レスポンス時に /login へリダイレクトする
 */
export const queryClient = new QueryClient({
  queryCache: new QueryCache({
    onError: (error) => {
      if (
        error instanceof HttpError &&
        error.status === 401 &&
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

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { authApi } from '@/api/auth'
import type { LoginRequest } from '@/types'

export const AUTH_QUERY_KEY = ['auth', 'me'] as const

export function useAuth() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const {
    data: user,
    isLoading,
    isError,
  } = useQuery({
    queryKey: AUTH_QUERY_KEY,
    queryFn: authApi.getMe,
    retry: false,
  })

  const loginMutation = useMutation({
    mutationFn: (request: LoginRequest) => authApi.login(request),
    onSuccess: (data) => {
      queryClient.setQueryData(AUTH_QUERY_KEY, data)
      navigate('/')
    },
  })

  const logoutMutation = useMutation({
    mutationFn: authApi.logout,
    onSuccess: () => {
      queryClient.clear()
      navigate('/login')
    },
  })

  return {
    user,
    isLoading,
    isAuthenticated: !!user && !isError,
    login: loginMutation.mutate,
    loginAsync: loginMutation.mutateAsync,
    loginError: loginMutation.error,
    isLoggingIn: loginMutation.isPending,
    logout: logoutMutation.mutate,
    isLoggingOut: logoutMutation.isPending,
  }
}

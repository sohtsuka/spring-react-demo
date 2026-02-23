import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router'
import { zodResolver } from '@hookform/resolvers/zod'
import axios from 'axios'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
} from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useAuth } from '@/hooks/useAuth'
import { loginSchema, type LoginFormValues } from '@/schemas/auth'

export function LoginPage() {
  const { loginAsync, isAuthenticated, isLoggingIn } = useAuth()
  const navigate = useNavigate()

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
  })

  // すでに認証済みの場合はダッシュボードへ
  useEffect(() => {
    if (isAuthenticated) {
      navigate('/', { replace: true })
    }
  }, [isAuthenticated, navigate])

  const onSubmit = async (data: LoginFormValues) => {
    try {
      await loginAsync(data)
    } catch (error) {
      if (axios.isAxiosError(error)) {
        const status = error.response?.status
        if (status === 401) {
          setError('root', {
            message: 'ユーザー名またはパスワードが正しくありません',
          })
        } else if (status === 423) {
          setError('root', {
            message:
              'アカウントがロックされています。しばらく経ってから再試行してください',
          })
        } else {
          setError('root', { message: 'ログインに失敗しました' })
        }
      }
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4">
      <Card className="w-full max-w-sm">
        <CardHeader className="space-y-1">
          <h1 className="text-2xl font-bold">ログイン</h1>
          <CardDescription>
            ユーザー名とパスワードを入力してください
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
            {errors.root && (
              <div className="rounded-md bg-destructive/10 px-4 py-3 text-sm text-destructive">
                {errors.root.message}
              </div>
            )}
            <div className="space-y-2">
              <Label htmlFor="username">ユーザー名</Label>
              <Input
                id="username"
                type="text"
                autoComplete="username"
                aria-invalid={!!errors.username}
                {...register('username')}
              />
              {errors.username && (
                <p className="text-xs text-destructive">
                  {errors.username.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="password">パスワード</Label>
              <Input
                id="password"
                type="password"
                autoComplete="current-password"
                aria-invalid={!!errors.password}
                {...register('password')}
              />
              {errors.password && (
                <p className="text-xs text-destructive">
                  {errors.password.message}
                </p>
              )}
            </div>
            <Button type="submit" className="w-full" disabled={isLoggingIn}>
              {isLoggingIn ? 'ログイン中...' : 'ログイン'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}

import { useAuth } from '@/hooks/useAuth'
import { Badge } from '@/components/ui/badge'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'

export function ProfilePage() {
  const { user } = useAuth()

  return (
    <div className="space-y-6 p-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">プロフィール</h1>
        <p className="text-muted-foreground">アカウント情報を確認できます</p>
      </div>

      <Card className="max-w-lg">
        <CardHeader>
          <CardTitle>ユーザー情報</CardTitle>
          <CardDescription>ログイン中のアカウント情報</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-1">
            <p className="text-sm font-medium text-muted-foreground">
              ユーザー ID
            </p>
            <p className="font-mono text-sm">{user?.id}</p>
          </div>
          <Separator />
          <div className="space-y-1">
            <p className="text-sm font-medium text-muted-foreground">
              ユーザー名
            </p>
            <p className="text-sm">{user?.username}</p>
          </div>
          <Separator />
          <div className="space-y-1">
            <p className="text-sm font-medium text-muted-foreground">
              メールアドレス
            </p>
            <p className="text-sm">{user?.email}</p>
          </div>
          <Separator />
          <div className="space-y-1">
            <p className="text-sm font-medium text-muted-foreground">ロール</p>
            <Badge variant="secondary">{user?.role}</Badge>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}

import { Link } from 'react-router'
import { Button } from '@/components/ui/button'

export function ForbiddenPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4">
      <h1 className="text-6xl font-bold text-muted-foreground">403</h1>
      <p className="text-xl font-semibold">アクセス権限がありません</p>
      <p className="text-muted-foreground">
        このページにアクセスする権限がありません
      </p>
      <Button asChild>
        <Link to="/">ダッシュボードへ戻る</Link>
      </Button>
    </div>
  )
}

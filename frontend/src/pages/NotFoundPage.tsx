import { Link } from 'react-router'
import { Button } from '@/components/ui/button'

export function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4">
      <h1 className="text-6xl font-bold text-muted-foreground">404</h1>
      <p className="text-xl font-semibold">ページが見つかりません</p>
      <p className="text-muted-foreground">
        お探しのページは存在しないか、移動された可能性があります
      </p>
      <Button asChild>
        <Link to="/">ダッシュボードへ戻る</Link>
      </Button>
    </div>
  )
}

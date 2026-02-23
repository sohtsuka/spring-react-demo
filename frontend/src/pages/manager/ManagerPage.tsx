import { useQuery } from '@tanstack/react-query'
import { usersApi } from '@/api/users'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'

export function ManagerPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['users'],
    queryFn: () => usersApi.getUsers({ page: 1, size: 20 }),
  })

  return (
    <div className="space-y-6 p-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">マネージャー画面</h1>
        <p className="text-muted-foreground">ユーザー一覧を確認できます</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>ユーザー一覧</CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading && (
            <div className="flex justify-center py-8">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
            </div>
          )}
          {isError && (
            <p className="text-center text-sm text-destructive">
              データの読み込みに失敗しました
            </p>
          )}
          {data && (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>ID</TableHead>
                    <TableHead>ユーザー名</TableHead>
                    <TableHead>メールアドレス</TableHead>
                    <TableHead>ロール</TableHead>
                    <TableHead>状態</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.data.map((user) => (
                    <TableRow key={user.id}>
                      <TableCell className="font-mono">{user.id}</TableCell>
                      <TableCell>{user.username}</TableCell>
                      <TableCell>{user.email}</TableCell>
                      <TableCell>
                        <Badge variant="secondary">{user.role}</Badge>
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant={user.enabled ? 'default' : 'destructive'}
                        >
                          {user.enabled ? '有効' : '無効'}
                        </Badge>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <p className="mt-4 text-sm text-muted-foreground">
                全 {data.pagination.totalElements} 件 / {data.pagination.page}{' '}
                ページ目
              </p>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

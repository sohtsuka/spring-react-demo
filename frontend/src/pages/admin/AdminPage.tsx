import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import { usersApi } from '@/api/users'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useToast } from '@/hooks/useToast'
import type { User } from '@/types'
import { UserFormDialog } from './UserFormDialog'

export function AdminPage() {
  const queryClient = useQueryClient()
  const { toast } = useToast()
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingUser, setEditingUser] = useState<User | null>(null)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['users'],
    queryFn: () => usersApi.getUsers({ page: 1, size: 20 }),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => usersApi.deleteUser(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['users'] })
      toast({ title: 'ユーザーを削除しました' })
    },
    onError: () => {
      toast({
        title: 'エラー',
        description: 'ユーザーの削除に失敗しました',
        variant: 'destructive',
      })
    },
  })

  const handleEdit = (user: User) => {
    setEditingUser(user)
    setDialogOpen(true)
  }

  const handleCreate = () => {
    setEditingUser(null)
    setDialogOpen(true)
  }

  const handleDelete = (user: User) => {
    if (confirm(`ユーザー「${user.username}」を削除しますか？`)) {
      deleteMutation.mutate(user.id)
    }
  }

  return (
    <div className="space-y-6 p-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">管理者画面</h1>
          <p className="text-muted-foreground">ユーザーの管理ができます</p>
        </div>
        <Button onClick={handleCreate}>
          <Plus className="mr-2 h-4 w-4" />
          ユーザー追加
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>ユーザー管理</CardTitle>
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
                    <TableHead className="text-right">操作</TableHead>
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
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-2">
                          <Button
                            variant="ghost"
                            size="icon"
                            aria-label="編集"
                            onClick={() => handleEdit(user)}
                          >
                            <Pencil className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            aria-label="削除"
                            onClick={() => handleDelete(user)}
                            disabled={deleteMutation.isPending}
                          >
                            <Trash2 className="h-4 w-4 text-destructive" />
                          </Button>
                        </div>
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

      <UserFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        editingUser={editingUser}
      />
    </div>
  )
}

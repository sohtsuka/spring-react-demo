import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import { usersApi } from '@/api/users'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { useToast } from '@/hooks/useToast'
import {
  createUserSchema,
  updateUserSchema,
  type CreateUserFormValues,
  type UpdateUserFormValues,
} from '@/schemas/user'
import type { User } from '@/types'

interface UserFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  editingUser: User | null
}

export function UserFormDialog({
  open,
  onOpenChange,
  editingUser,
}: UserFormDialogProps) {
  const queryClient = useQueryClient()
  const { toast } = useToast()
  const isEditing = editingUser !== null

  // 作成・更新でスキーマを切り替え
  const createForm = useForm<CreateUserFormValues>({
    resolver: zodResolver(createUserSchema),
    defaultValues: { role: 'USER' },
  })

  const updateForm = useForm<UpdateUserFormValues>({
    resolver: zodResolver(updateUserSchema),
  })

  useEffect(() => {
    if (editingUser) {
      updateForm.reset({
        username: editingUser.username,
        email: editingUser.email,
        role: editingUser.role,
        enabled: editingUser.enabled,
      })
    } else {
      createForm.reset({ role: 'USER' })
    }
  }, [editingUser, createForm, updateForm])

  const createMutation = useMutation({
    mutationFn: usersApi.createUser,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['users'] })
      toast({ title: 'ユーザーを作成しました' })
      onOpenChange(false)
    },
    onError: () => {
      toast({
        title: 'エラー',
        description: 'ユーザーの作成に失敗しました',
        variant: 'destructive',
      })
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateUserFormValues }) =>
      usersApi.updateUser(id, data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['users'] })
      toast({ title: 'ユーザーを更新しました' })
      onOpenChange(false)
    },
    onError: () => {
      toast({
        title: 'エラー',
        description: 'ユーザーの更新に失敗しました',
        variant: 'destructive',
      })
    },
  })

  const isPending = createMutation.isPending || updateMutation.isPending

  if (isEditing) {
    return (
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>ユーザー編集</DialogTitle>
            <DialogDescription>ユーザー情報を編集します</DialogDescription>
          </DialogHeader>
          <form
            onSubmit={updateForm.handleSubmit((data) =>
              updateMutation.mutate({ id: editingUser.id, data }),
            )}
            className="space-y-4"
          >
            <div className="space-y-2">
              <Label htmlFor="edit-username">ユーザー名</Label>
              <Input
                id="edit-username"
                {...updateForm.register('username')}
                aria-invalid={!!updateForm.formState.errors.username}
              />
              {updateForm.formState.errors.username && (
                <p className="text-xs text-destructive">
                  {updateForm.formState.errors.username.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="edit-email">メールアドレス</Label>
              <Input
                id="edit-email"
                type="email"
                {...updateForm.register('email')}
                aria-invalid={!!updateForm.formState.errors.email}
              />
              {updateForm.formState.errors.email && (
                <p className="text-xs text-destructive">
                  {updateForm.formState.errors.email.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="edit-role">ロール</Label>
              <Select id="edit-role" {...updateForm.register('role')}>
                <option value="USER">USER</option>
                <option value="MANAGER">MANAGER</option>
                <option value="ADMIN">ADMIN</option>
              </Select>
            </div>
            <div className="flex items-center gap-2">
              <input
                id="edit-enabled"
                type="checkbox"
                className="h-4 w-4 rounded border-input"
                {...updateForm.register('enabled')}
              />
              <Label htmlFor="edit-enabled">アカウント有効</Label>
            </div>
            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
              >
                キャンセル
              </Button>
              <Button type="submit" disabled={isPending}>
                {isPending ? '更新中...' : '更新'}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    )
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>ユーザー作成</DialogTitle>
          <DialogDescription>新しいユーザーを作成します</DialogDescription>
        </DialogHeader>
        <form
          onSubmit={createForm.handleSubmit((data) =>
            createMutation.mutate(data),
          )}
          className="space-y-4"
        >
          <div className="space-y-2">
            <Label htmlFor="create-username">ユーザー名</Label>
            <Input
              id="create-username"
              {...createForm.register('username')}
              aria-invalid={!!createForm.formState.errors.username}
            />
            {createForm.formState.errors.username && (
              <p className="text-xs text-destructive">
                {createForm.formState.errors.username.message}
              </p>
            )}
          </div>
          <div className="space-y-2">
            <Label htmlFor="create-email">メールアドレス</Label>
            <Input
              id="create-email"
              type="email"
              {...createForm.register('email')}
              aria-invalid={!!createForm.formState.errors.email}
            />
            {createForm.formState.errors.email && (
              <p className="text-xs text-destructive">
                {createForm.formState.errors.email.message}
              </p>
            )}
          </div>
          <div className="space-y-2">
            <Label htmlFor="create-password">パスワード</Label>
            <Input
              id="create-password"
              type="password"
              {...createForm.register('password')}
              aria-invalid={!!createForm.formState.errors.password}
            />
            {createForm.formState.errors.password && (
              <p className="text-xs text-destructive">
                {createForm.formState.errors.password.message}
              </p>
            )}
          </div>
          <div className="space-y-2">
            <Label htmlFor="create-role">ロール</Label>
            <Select id="create-role" {...createForm.register('role')}>
              <option value="USER">USER</option>
              <option value="MANAGER">MANAGER</option>
              <option value="ADMIN">ADMIN</option>
            </Select>
          </div>
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
            >
              キャンセル
            </Button>
            <Button type="submit" disabled={isPending}>
              {isPending ? '作成中...' : '作成'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

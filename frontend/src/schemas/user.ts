import { z } from 'zod'

const roleEnum = z.enum(['ADMIN', 'MANAGER', 'USER'])

const passwordSchema = z
  .string()
  .min(8, 'パスワードは8文字以上で入力してください')
  .regex(/[A-Z]/, '英大文字を1文字以上含めてください')
  .regex(/[a-z]/, '英小文字を1文字以上含めてください')
  .regex(/[0-9]/, '数字を1文字以上含めてください')
  .regex(/[^A-Za-z0-9]/, '記号を1文字以上含めてください')

export const createUserSchema = z.object({
  username: z
    .string()
    .min(1, 'ユーザー名を入力してください')
    .max(50, 'ユーザー名は50文字以内で入力してください'),
  email: z
    .string()
    .min(1, 'メールアドレスを入力してください')
    .email('メールアドレスの形式が正しくありません')
    .max(255, 'メールアドレスは255文字以内で入力してください'),
  password: passwordSchema,
  role: roleEnum,
})

export const updateUserSchema = z.object({
  username: z
    .string()
    .min(1, 'ユーザー名を入力してください')
    .max(50, 'ユーザー名は50文字以内で入力してください'),
  email: z
    .string()
    .min(1, 'メールアドレスを入力してください')
    .email('メールアドレスの形式が正しくありません')
    .max(255, 'メールアドレスは255文字以内で入力してください'),
  role: roleEnum,
  enabled: z.boolean(),
})

export type CreateUserFormValues = z.infer<typeof createUserSchema>
export type UpdateUserFormValues = z.infer<typeof updateUserSchema>

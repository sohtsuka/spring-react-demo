import { z } from 'zod'

export const loginSchema = z.object({
  username: z
    .string()
    .min(1, 'ユーザー名を入力してください')
    .max(50, 'ユーザー名は50文字以内で入力してください'),
  password: z
    .string()
    .min(1, 'パスワードを入力してください')
    .min(8, 'パスワードは8文字以上で入力してください'),
})

export type LoginFormValues = z.infer<typeof loginSchema>

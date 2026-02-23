import { type Page } from '@playwright/test'

/**
 * ログイン済み状態をセットアップするヘルパー
 */
export async function loginAs(
  page: Page,
  credentials: { username: string; password: string },
) {
  await page.goto('/login')
  await page.getByLabel('ユーザー名').fill(credentials.username)
  await page.getByLabel('パスワード').fill(credentials.password)
  await page.getByRole('button', { name: 'ログイン' }).click()
  await page.waitForURL('/')
}

export const adminCredentials = {
  username: 'admin',
  password: 'Password1!',
}

export const managerCredentials = {
  username: 'manager',
  password: 'Password1!',
}

export const userCredentials = {
  username: 'user1',
  password: 'Password1!',
}

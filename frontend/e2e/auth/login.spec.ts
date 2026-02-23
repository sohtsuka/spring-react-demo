import { expect, test } from '@playwright/test'
import { adminCredentials } from '../fixtures/auth'

test.describe('ログイン画面', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
  })

  test('ログインフォームが表示される', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'ログイン' })).toBeVisible()
    await expect(page.getByLabel('ユーザー名')).toBeVisible()
    await expect(page.getByLabel('パスワード')).toBeVisible()
    await expect(page.getByRole('button', { name: 'ログイン' })).toBeVisible()
  })

  test('空のフォームを送信するとバリデーションエラーが表示される', async ({
    page,
  }) => {
    await page.getByRole('button', { name: 'ログイン' }).click()
    await expect(
      page.getByText('ユーザー名を入力してください'),
    ).toBeVisible()
  })

  test('短いパスワードでバリデーションエラーが表示される', async ({
    page,
  }) => {
    await page.getByLabel('ユーザー名').fill('testuser')
    await page.getByLabel('パスワード').fill('short')
    await page.getByRole('button', { name: 'ログイン' }).click()
    await expect(
      page.getByText('パスワードは8文字以上で入力してください'),
    ).toBeVisible()
  })

  test('誤った認証情報でエラーメッセージが表示される', async ({ page }) => {
    await page.getByLabel('ユーザー名').fill('wronguser')
    await page.getByLabel('パスワード').fill('wrongpassword')
    await page.getByRole('button', { name: 'ログイン' }).click()
    await expect(
      page.getByText('ユーザー名またはパスワードが正しくありません'),
    ).toBeVisible()
  })

  test('正しい認証情報でダッシュボードへ遷移する', async ({ page }) => {
    await page.getByLabel('ユーザー名').fill(adminCredentials.username)
    await page.getByLabel('パスワード').fill(adminCredentials.password)
    await page.getByRole('button', { name: 'ログイン' }).click()
    await page.waitForURL('/')
    await expect(
      page.getByRole('heading', { name: 'ダッシュボード' }),
    ).toBeVisible()
  })

  test('認証済みの場合は /login でダッシュボードへリダイレクトされる', async ({
    page,
  }) => {
    // まずログイン
    await page.getByLabel('ユーザー名').fill(adminCredentials.username)
    await page.getByLabel('パスワード').fill(adminCredentials.password)
    await page.getByRole('button', { name: 'ログイン' }).click()
    await page.waitForURL('/')

    // /login へアクセスすると / へリダイレクト
    await page.goto('/login')
    await expect(page).toHaveURL('/')
  })
})

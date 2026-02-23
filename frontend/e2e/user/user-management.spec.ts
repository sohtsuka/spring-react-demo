import { expect, test } from '@playwright/test'
import { adminCredentials, loginAs, userCredentials } from '../fixtures/auth'

test.describe('ユーザー管理 (管理者操作)', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, adminCredentials)
    await page.goto('/admin')
  })

  test('ユーザー一覧が表示される', async ({ page }) => {
    await expect(
      page.getByRole('heading', { name: '管理者画面' }),
    ).toBeVisible()
    await expect(page.getByRole('table')).toBeVisible()
  })

  test('ユーザー追加ダイアログを開ける', async ({ page }) => {
    await page.getByRole('button', { name: 'ユーザー追加' }).click()
    await expect(
      page.getByRole('dialog', { name: 'ユーザー作成' }),
    ).toBeVisible()
  })

  test('必須項目が空だとユーザー作成できない', async ({ page }) => {
    await page.getByRole('button', { name: 'ユーザー追加' }).click()
    await page.getByRole('button', { name: '作成' }).click()
    await expect(
      page.getByText('ユーザー名を入力してください'),
    ).toBeVisible()
  })

  test('新規ユーザーを作成できる', async ({ page }) => {
    const newUsername = `testuser_${Date.now()}`
    await page.getByRole('button', { name: 'ユーザー追加' }).click()

    await page.getByLabel('ユーザー名').fill(newUsername)
    await page.getByLabel('メールアドレス').fill(`${newUsername}@example.com`)
    await page.getByLabel('パスワード').fill('Password1!')
    await page.getByRole('button', { name: '作成' }).click()

    await expect(page.getByText('ユーザーを作成しました', { exact: true })).toBeVisible()
    await expect(page.getByRole('cell', { name: newUsername, exact: true })).toBeVisible()
  })

  test('ユーザーを編集できる', async ({ page }) => {
    // 最初のデータ行の編集ボタンをクリック
    await page.getByRole('row').nth(1).getByRole('button', { name: '編集' }).click()
    await expect(
      page.getByRole('dialog', { name: 'ユーザー編集' }),
    ).toBeVisible()
  })
})

test.describe('管理者画面へのアクセス制御', () => {
  test('一般ユーザーは /admin にアクセスできない', async ({ page }) => {
    await loginAs(page, userCredentials)
    await page.goto('/admin')
    await expect(page).toHaveURL('/403')
    await expect(
      page.getByText('アクセス権限がありません'),
    ).toBeVisible()
  })
})

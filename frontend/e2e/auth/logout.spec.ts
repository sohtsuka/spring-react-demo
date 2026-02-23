import { expect, test } from '@playwright/test'
import { adminCredentials, loginAs } from '../fixtures/auth'

test.describe('ログアウト', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, adminCredentials)
  })

  test('ログアウトボタンでログアウトできる', async ({ page }) => {
    await page.getByTitle('ログアウト').click()
    await page.waitForURL('/login')
    await expect(
      page.getByRole('heading', { name: 'ログイン' }),
    ).toBeVisible()
  })

  test('ログアウト後に / へアクセスすると /login へリダイレクトされる', async ({
    page,
  }) => {
    await page.getByTitle('ログアウト').click()
    await page.waitForURL('/login')
    await page.goto('/')
    await expect(page).toHaveURL('/login')
  })
})

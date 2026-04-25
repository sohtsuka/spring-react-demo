import { expect, test } from '@playwright/test'
import { adminCredentials, loginAs } from '../fixtures/auth'

test.describe('オンラインバッチデモ', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, adminCredentials)
    await page.goto('/batch-demo')
  })

  test('オンラインバッチ画面を表示できる', async ({ page }) => {
    await expect(
      page.getByRole('heading', { name: 'オンラインバッチデモ' }),
    ).toBeVisible()
    await expect(page.getByLabel('ジョブ名')).toBeVisible()
    await expect(page.getByText('ジョブ一覧', { exact: true })).toBeVisible()
    await expect(page.getByText('ジョブ詳細', { exact: true })).toBeVisible()
  })

  test('成功するジョブを起動して完了まで確認できる', async ({ page }) => {
    const jobName = `成功デモ_${Date.now()}`

    await page.getByLabel('ジョブ名').fill(jobName)
    await page.getByLabel('処理件数').fill('2')
    await page.getByLabel('処理間隔(ms)').fill('100')
    await page.getByLabel('失敗させる件番').fill('')
    await page.getByRole('button', { name: 'バッチを起動' }).click()

    await expect(
      page.getByText('オンラインバッチを受け付けました', { exact: true }),
    ).toBeVisible()
    await expect(page.getByRole('button', { name: /更新/ })).toBeVisible()
    await expect(page.getByRole('button', { name: new RegExp(jobName) })).toBeVisible()
    await expect(page.getByText(`現在処理中: 待機または完了`)).toBeVisible({
      timeout: 10000,
    })
    await expect(
      page.getByRole('button', { name: new RegExp(`${jobName}.*完了`) }),
    ).toBeVisible({ timeout: 10000 })
    await expect(page.getByText('100%')).toBeVisible({ timeout: 10000 })
    await expect(page.getByText('すべてのデータを処理しました')).toBeVisible({
      timeout: 10000,
    })
  })

  test('失敗するジョブを起動して失敗状態を確認できる', async ({ page }) => {
    const jobName = `失敗デモ_${Date.now()}`

    await page.getByLabel('ジョブ名').fill(jobName)
    await page.getByLabel('処理件数').fill('3')
    await page.getByLabel('処理間隔(ms)').fill('100')
    await page.getByLabel('失敗させる件番').fill('2')
    await page.getByRole('button', { name: 'バッチを起動' }).click()

    await expect(page.getByRole('button', { name: new RegExp(jobName) })).toBeVisible()
    await expect(
      page.getByRole('button', { name: new RegExp(`${jobName}.*失敗`) }),
    ).toBeVisible({ timeout: 10000 })
    await expect(page.getByText('66%')).toBeVisible({ timeout: 10000 })
    await expect(
      page.getByText('データ2/3 でエラーが発生し、ジョブを停止しました'),
    ).toBeVisible({ timeout: 10000 })
  })
})

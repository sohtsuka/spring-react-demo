// @ts-check
/**
 * StrykerJS — ミューテーションテスト (テストの検出力を測る)
 * 実行: pnpm mutation  → reports/mutation/mutation.html
 * 実行時間が長いため通常の CI には含めず、mutation-test ワークフロー (手動 / 週次) で実行する。
 * @type {import('@stryker-mutator/api/core').PartialStrykerOptions}
 */
export default {
  packageManager: 'pnpm',
  // pnpm の厳格な node_modules では自動検出されないため明示する
  plugins: ['@stryker-mutator/vitest-runner', '@stryker-mutator/typescript-checker'],
  testRunner: 'vitest',
  vitest: { configFile: 'vite.config.ts' },
  // 型エラーになるミュータントはテスト実行前に除外する
  checkers: ['typescript'],
  tsconfigFile: 'tsconfig.app.json',
  // カバレッジ (vite.config.ts) と同じ除外
  mutate: [
    'src/**/*.{ts,tsx}',
    '!src/main.tsx',
    '!src/vite-env.d.ts',
    '!src/types/**',
    '!src/components/ui/**',
    '!src/**/__tests__/**',
    '!src/**/*.test.{ts,tsx}',
    '!src/test/**',
  ],
  // ミュータントを覆うテストだけを実行して高速化する
  coverageAnalysis: 'perTest',
  reporters: ['clear-text', 'progress', 'html', 'json'],
  htmlReporter: { fileName: 'reports/mutation/mutation.html' },
  jsonReporter: { fileName: 'reports/mutation/mutation.json' },
  incrementalFile: 'reports/stryker-incremental.json',
  // 初回導入時は閾値で落とさず、レポートで傾向を見る
  thresholds: { high: 80, low: 60, break: null },
  tempDirName: '.stryker-tmp',
}

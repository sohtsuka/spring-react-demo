# Frontend

Vite + React + TypeScript による SPA フロントエンドです。

APIのエラー本文は `HttpError` の `code` / `message` / `details` に保持します。ログイン画面はHTTPステータスとcodeを組み合わせ、401の `ACCOUNT_LOCKED` をロック案内、`ACCOUNT_DISABLED` を無効化の案内として表示します。429の `RATE_LIMIT_EXCEEDED` もAPIのメッセージを表示します。この契約はMSWでAPIクライアント・useAuth・LoginPageを通して検証します。

## 前提条件

- Node.js 24.x (LTS)
- pnpm 12.7.0 (`package.json` で固定)
- バックエンド (`http://localhost:8080`) が起動していること (API 通信時)

## セットアップ

```bash
pnpm install
```

依存更新は Renovate と同じく公開から 7 日以上経過した GA 版を対象とします。
`pnpm-workspace.yaml` の `minimumReleaseAge` でもこの制約を適用しています。
TypeScript は typescript-eslint の対応範囲 (`<6.1.0`) に合わせて 6.0.3 を使用し、
`@types/node` は実行環境と同じ 24 系を使用します。

## 開発サーバー起動

```bash
pnpm dev
```

ブラウザで `http://localhost:5173` を開いてください。
`/api/*` へのリクエストは自動的に `http://localhost:8080` へプロキシされます。

## ビルド

```bash
pnpm build
```

`dist/` に静的ファイルが出力されます。

## テスト

### ユニット・コンポーネントテスト (Vitest)

```bash
# 通常実行
pnpm test

# ウォッチモード
pnpm test --watch

# カバレッジレポート生成 (build/reports/coverage/)
pnpm test:coverage
```

### E2E テスト (Playwright)

E2E テストはフロントエンドとバックエンドの両方が起動している状態で実行してください。

```bash
# Playwright ブラウザのインストール (初回のみ)
pnpm exec playwright install

# E2E テスト実行 (Chromium / Firefox / WebKit)
pnpm e2e

# UI モードで実行 (ステップ確認・デバッグ用)
pnpm e2e:ui
```

テストレポートは `playwright-report/` に出力されます。

## ディレクトリ構成

```
src/
├── api/            API クライアント関数 (fetch ラッパー)
├── components/
│   ├── ui/         shadcn/ui コンポーネント
│   └── common/     ProtectedRoute, RoleProtectedRoute, Layout
├── hooks/          カスタムフック (useAuth, useToast)
├── lib/            fetch ラッパーと HttpError, QueryClient, utils
├── pages/          ページコンポーネント
│   ├── admin/      管理者画面 (ADMIN ロール専用)
│   └── manager/    マネージャー画面 (ADMIN / MANAGER ロール)
├── schemas/        Zod バリデーションスキーマ
└── types/          TypeScript 型定義

e2e/
├── auth/           ログイン・ログアウトの E2E テスト
├── user/           ユーザー管理の E2E テスト
└── fixtures/       認証ヘルパー
```

## ページ一覧

| パス | ページ | 認証 | 必要ロール |
|---|---|---|---|
| `/login` | ログイン | 不要 | — |
| `/` | ダッシュボード | 要 | 全ロール |
| `/profile` | プロフィール | 要 | 全ロール |
| `/manager` | マネージャー画面 | 要 | ADMIN, MANAGER |
| `/admin` | 管理者画面 | 要 | ADMIN |
| `/403` | 権限エラー | — | — |
| `/404` | Not Found | — | — |

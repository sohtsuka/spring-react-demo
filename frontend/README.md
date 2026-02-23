# Frontend

Vite + React + TypeScript による SPA フロントエンドです。

## 前提条件

- Node.js 24.x (LTS)
- pnpm 10.x
- バックエンド (`http://localhost:8080`) が起動していること (API 通信時)

## セットアップ

```bash
pnpm install
```

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
├── api/            API クライアント関数 (axios ラッパー)
├── components/
│   ├── ui/         shadcn/ui コンポーネント
│   └── common/     ProtectedRoute, RoleProtectedRoute, Layout
├── hooks/          カスタムフック (useAuth, useToast)
├── lib/            axios インスタンス, QueryClient, utils
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

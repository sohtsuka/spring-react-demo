# フロントエンド C1 カバレッジ 100% 達成計画

## 現状

### 既存テスト
| ファイル | テスト対象 |
|---|---|
| `src/lib/__tests__/utils.test.ts` | `cn()` 関数 |
| `src/schemas/__tests__/auth.test.ts` | `loginSchema` |
| `src/schemas/__tests__/user.test.ts` | `createUserSchema`, `updateUserSchema` |

### カバレッジ対象外とするファイル
- `src/main.tsx` - エントリーポイント（DOMマウントのみ）
- `src/components/ui/**` - shadcn/ui 自動生成コンポーネント
- `src/vite-env.d.ts` - 型定義のみ
- `src/types/index.ts` - 型定義のみ

---

## Step 1: MSW のインストールとセットアップ

### インストール
```sh
pnpm add -D msw
```

MSW は `msw/node` を使って Node.js（jsdom）環境でリクエストをインターセプトする。
ブラウザ用の Service Worker は不要。

### `src/test/handlers.ts`（新規）
```ts
import { http, HttpResponse } from 'msw'
import { mockAuthUser, mockUser } from './mockUser'

export const handlers = [
  http.get('/api/v1/auth/me', () => HttpResponse.json({ data: mockAuthUser })),
  http.post('/api/v1/auth/login', () => HttpResponse.json({ data: mockAuthUser })),
  http.post('/api/v1/auth/logout', () => new HttpResponse(null, { status: 200 })),
  http.get('/api/v1/users', () =>
    HttpResponse.json({
      data: [mockUser],
      pagination: { page: 1, size: 20, totalElements: 1, totalPages: 1 },
    })
  ),
  http.get('/api/v1/users/:id', () => HttpResponse.json({ data: mockUser })),
  http.post('/api/v1/users', () => HttpResponse.json({ data: mockUser }, { status: 201 })),
  http.put('/api/v1/users/:id', () => HttpResponse.json({ data: mockUser })),
  http.delete('/api/v1/users/:id', () => new HttpResponse(null, { status: 204 })),
]
```

### `src/test/server.ts`（新規）
```ts
import { setupServer } from 'msw/node'
import { handlers } from './handlers'

export const server = setupServer(...handlers)
```

### `src/test/setup.ts` の更新
```ts
import '@testing-library/jest-dom'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { server } from './server'

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
```

各テストで `server.use(...)` を使ってハンドラーを上書きすることでエラーケース等を再現する。

---

## Step 2: vite.config.ts にカバレッジ設定を追加

```ts
test: {
  globals: true,
  environment: 'jsdom',
  setupFiles: ['./src/test/setup.ts'],
  exclude: ['**/node_modules/**', '**/e2e/**'],
  coverage: {
    provider: 'v8',
    include: ['src/**/*.{ts,tsx}'],
    exclude: [
      'src/main.tsx',
      'src/vite-env.d.ts',
      'src/types/**',
      'src/components/ui/**',
      'src/**/__tests__/**',
      'src/test/**',
    ],
    thresholds: {
      branches: 100,
      lines: 100,
      functions: 100,
      statements: 100,
    },
  },
},
```

---

## Step 3: 新規テストファイル一覧と対象ブランチ

### 3-1. `src/lib/__tests__/axios.test.ts` （新規）

対象: `src/lib/axios.ts`

MSW で `/api/v1/*` へのリクエストをインターセプトし、受信したリクエストオブジェクトを検査することで
インターセプターの動作を実際の HTTP スタックを通して検証する。

| ブランチ | テストケース |
|---|---|
| `getCookieValue`: クッキーが存在する | `document.cookie` に該当キーがある場合に値を返す |
| `getCookieValue`: クッキーが存在しない | 該当キーがない場合に `null` を返す |
| リクエストインターセプター: 非安全メソッド + CSRF トークンあり | `document.cookie` に `XSRF-TOKEN` をセットして POST → MSW で受信したリクエストの `X-CSRF-TOKEN` ヘッダーを確認 |
| リクエストインターセプター: 非安全メソッド + CSRF トークンなし | クッキーなしで POST → `X-CSRF-TOKEN` ヘッダーが付与されないことを確認 |
| リクエストインターセプター: 安全メソッド (GET) | GET リクエスト → `X-CSRF-TOKEN` ヘッダーが付与されないことを確認 |
| レスポンスインターセプター: 成功 | MSW が 200 を返す → レスポンスがそのまま解決される |
| レスポンスインターセプター: エラー | MSW が 500 を返す → `Promise.reject` で伝搬する |

---

### 3-2. `src/lib/__tests__/queryClient.test.ts` （新規）

対象: `src/lib/queryClient.ts`

MSW で `/api/v1/auth/me` などに 401 を返すハンドラーをセットし、`queryClient` に実際のクエリを実行させる。
`window.location.href` は `Object.defineProperty` でモックする。

| ブランチ | テストケース |
|---|---|
| `onError`: Axios エラー + 401 + `/login` 以外のパス | MSW が 401 → `window.location.href` が `/login` になる |
| `onError`: Axios エラー + 401 + `/login` パス | `pathname` が `/login` の場合はリダイレクトしない |
| `onError`: Axios エラー + 非 401 | MSW が 500 → リダイレクトしない |
| `onError`: 非 Axios エラー | ネットワークエラー → リダイレクトしない |

---

### 3-3. `src/api/__tests__/auth.test.ts` （新規）

対象: `src/api/auth.ts`

MSW のデフォルトハンドラーで `/api/v1/auth/*` をインターセプト。実際の axios インスタンスを通して呼び出す。

| テストケース |
|---|
| `authApi.login` 成功 → `response.data.data` を返す |
| `authApi.logout` 成功 → void を返す |
| `authApi.getMe` 成功 → `response.data.data` を返す |

---

### 3-4. `src/api/__tests__/users.test.ts` （新規）

対象: `src/api/users.ts`

MSW のデフォルトハンドラーで `/api/v1/users/*` をインターセプト。実際の axios インスタンスを通して呼び出す。

| テストケース |
|---|
| `usersApi.getUsers` パラメータあり → クエリパラメータが MSW に届き `response.data` を返す |
| `usersApi.getUsers` パラメータなし（デフォルト `{}`）→ `response.data` を返す |
| `usersApi.getUser(id)` 成功 → `response.data.data` を返す |
| `usersApi.createUser` 成功 → `response.data.data` を返す |
| `usersApi.updateUser` 成功 → `response.data.data` を返す |
| `usersApi.deleteUser` 成功 → void を返す |

---

### 3-5. `src/hooks/__tests__/useToast.test.ts` （新規）

対象: `src/hooks/useToast.ts`

`reducer` を直接テストし、`toast()` と `useToast()` は `renderHook` + `act` でテスト。

#### `reducer` のブランチ

| ケース | 内容 |
|---|---|
| `ADD_TOAST` | トーストを追加し、`TOAST_LIMIT (1)` でスライス |
| `ADD_TOAST` 上限超過 | 2 件目追加時に 1 件に切り詰められる |
| `UPDATE_TOAST` 一致 ID | 対象トーストを更新する |
| `UPDATE_TOAST` 不一致 ID | 他のトーストは変更されない |
| `DISMISS_TOAST` toastId あり | 指定トーストのみ `open: false` |
| `DISMISS_TOAST` toastId なし | 全トーストを `open: false` |
| `REMOVE_TOAST` toastId あり | 指定トーストを削除 |
| `REMOVE_TOAST` toastId なし | 全トーストを削除 |

#### `addToRemoveQueue` のブランチ

| ケース |
|---|
| 同一 ID が既にキューにある場合 → 二重登録しない |
| キューにない場合 → タイムアウト登録し、タイムアウト後に REMOVE_TOAST |

#### `toast()` のブランチ

| ケース |
|---|
| `onOpenChange(false)` → `dismiss()` が呼ばれる |
| `onOpenChange(true)` → `dismiss()` は呼ばれない |

#### `useToast()` のブランチ

| ケース |
|---|
| リスナー登録 → state 変化が反映される |
| クリーンアップ → listeners から削除される |
| `dismiss(toastId)` → DISMISS_TOAST をディスパッチ |
| `dismiss()` 引数なし → toastId undefined でディスパッチ |

---

### 3-6. `src/hooks/__tests__/useAuth.test.ts` （新規）

対象: `src/hooks/useAuth.ts`

`QueryClientProvider` + `MemoryRouter` で包み、MSW で `/api/v1/auth/*` をインターセプト。
`useNavigate` は `MemoryRouter` 内で実際に動作させ、遷移先を `useLocation` で確認する。

| ブランチ | テストケース |
|---|---|
| `isAuthenticated`: MSW が `getMe` で user を返す | `true` |
| `isAuthenticated`: MSW が `getMe` で 401 を返す | `false`（isError = true）|
| `isAuthenticated`: user なし（getMe が undefined を返す）| `false` |
| `loginMutation.onSuccess` | MSW が `login` で user を返す → queryData をセットし `/` へ遷移 |
| `logoutMutation.onSuccess` | MSW が `logout` で 200 を返す → queryClient.clear() し `/login` へ遷移 |

---

### 3-7. `src/components/common/__tests__/ProtectedRoute.test.tsx` （新規）

対象: `src/components/common/ProtectedRoute.tsx`

`useAuth` を `vi.mock`（コンポーネントの UI ロジックのみテストするため）。

| ブランチ | 期待する表示 |
|---|---|
| `isLoading: true` | スピナーが表示される |
| `isAuthenticated: false` | `/login` へリダイレクト (`<Navigate>`) |
| `isAuthenticated: true` | `<Outlet>` が描画される |

---

### 3-8. `src/components/common/__tests__/RoleProtectedRoute.test.tsx` （新規）

対象: `src/components/common/RoleProtectedRoute.tsx`

`useAuth` を `vi.mock`（コンポーネントの UI ロジックのみテストするため）。

| ブランチ | 期待する表示 |
|---|---|
| `isLoading: true` | スピナーが表示される |
| `user: null` | `/login` へリダイレクト |
| `user.role` が `allowedRoles` に含まれない | `/403` へリダイレクト |
| `user.role` が `allowedRoles` に含まれる | `<Outlet>` が描画される |

---

### 3-9. `src/components/common/__tests__/Layout.test.tsx` （新規）

対象: `src/components/common/Layout.tsx`

`useAuth` を `vi.mock`（ナビゲーション表示ロジックのみテスト）、`MemoryRouter` + `Routes` で包む。

| ブランチ | テストケース |
|---|---|
| `visibleNavItems` filter: `item.roles` なし | 常に表示される |
| `visibleNavItems` filter: `item.roles` あり + `user` なし | 表示されない |
| `visibleNavItems` filter: `item.roles` あり + ロール一致 | 表示される |
| `visibleNavItems` filter: `item.roles` あり + ロール不一致 | 表示されない |
| `initials`: `user.username` あり | 先頭 2 文字大文字 |
| `initials`: `user` undefined | `'??'` |
| `isActive`: `href='/'` + `pathname='/'` | active スタイル |
| `isActive`: `href='/'` + `pathname='/other'` | inactive スタイル |
| `isActive`: `href='/profile'` + `pathname='/profile'` | active スタイル |
| `isActive`: `href='/profile'` + `pathname='/'` | inactive スタイル |
| ログアウトボタンクリック | `logout()` が呼ばれる |
| `isLoggingOut: true` | ログアウトボタンが `disabled` |

---

### 3-10. `src/pages/__tests__/LoginPage.test.tsx` （新規）

対象: `src/pages/LoginPage.tsx`

`QueryClientProvider` + `MemoryRouter` で包む。MSW で `/api/v1/auth/login` の返却値を制御する。
`/api/v1/auth/me` は 401 を返すハンドラー（未認証状態）をデフォルトとする。

| ブランチ | テストケース |
|---|---|
| `isAuthenticated: true` | MSW が `getMe` で user を返す → `useEffect` で `/` へ遷移 |
| `isAuthenticated: false` | MSW が `getMe` で 401 → 遷移しない |
| フォーム送信: 成功 | MSW が `login` で 200 → ダッシュボードへ遷移 |
| フォーム送信: 401 エラー | `server.use(http.post('/api/v1/auth/login', () => HttpResponse.json({}, {status: 401})))` → root エラーに認証失敗メッセージ |
| フォーム送信: 423 エラー | 同様に 423 を返す → アカウントロックメッセージ |
| フォーム送信: その他エラー | 500 を返す → 汎用メッセージ |
| `errors.root` あり | エラーメッセージが表示される |
| `errors.username` あり | フィールドエラーが表示される（バリデーション） |
| `errors.password` あり | フィールドエラーが表示される（バリデーション） |
| `isLoggingIn: true` | ボタンに「ログイン中...」（ペンディング状態を待機） |
| `isLoggingIn: false` | ボタンに「ログイン」 |

---

### 3-11. `src/pages/__tests__/DashboardPage.test.tsx` （新規）

対象: `src/pages/DashboardPage.tsx`

`useAuth` を `vi.mock`（表示内容のみテスト、API 呼び出しなし）。

| テストケース |
|---|
| user が存在する場合にユーザー名・メール・ロールを表示 |
| `user` が undefined の場合（省略表示）|

---

### 3-12. `src/pages/__tests__/ProfilePage.test.tsx` （新規）

対象: `src/pages/ProfilePage.tsx`

`useAuth` を `vi.mock`（表示内容のみテスト、API 呼び出しなし）。

| テストケース |
|---|
| user が存在する場合に全フィールドを表示 |
| `user` が undefined の場合（省略表示）|

---

### 3-13. `src/pages/__tests__/ForbiddenPage.test.tsx` （新規）

対象: `src/pages/ForbiddenPage.tsx`

| テストケース |
|---|
| 403 テキストが表示される |
| ダッシュボードへのリンクが存在する |

---

### 3-14. `src/pages/__tests__/NotFoundPage.test.tsx` （新規）

対象: `src/pages/NotFoundPage.tsx`

| テストケース |
|---|
| 404 テキストが表示される |
| ダッシュボードへのリンクが存在する |

---

### 3-15. `src/pages/manager/__tests__/ManagerPage.test.tsx` （新規）

対象: `src/pages/manager/ManagerPage.tsx`

`QueryClientProvider` + `MemoryRouter` で包む。MSW で `/api/v1/users` の返却値を制御する。

| ブランチ | テストケース |
|---|---|
| `isLoading: true` | スピナーが表示される |
| `isError: true` | エラーメッセージが表示される |
| `data` あり | テーブルが描画される |
| `user.enabled: true` | バッジに「有効」と表示 |
| `user.enabled: false` | バッジに「無効」と表示 |

---

### 3-16. `src/pages/admin/__tests__/AdminPage.test.tsx` （新規）

対象: `src/pages/admin/AdminPage.tsx`

`QueryClientProvider` + `MemoryRouter` で包む。MSW で `/api/v1/users/*` の返却値を制御する。
`window.confirm` は `vi.spyOn` でモックする。

| ブランチ | テストケース |
|---|---|
| `isLoading: true` | スピナーが表示される |
| `isError: true` | エラーメッセージが表示される |
| `data` あり | テーブルが描画される |
| `user.enabled: true` | バッジ variant が `default` |
| `user.enabled: false` | バッジ variant が `destructive` |
| 編集ボタンクリック | `UserFormDialog` に `editingUser` が渡される |
| 追加ボタンクリック | `UserFormDialog` が `editingUser: null` で開く |
| 削除確認: OK | `usersApi.deleteUser` が呼ばれる |
| 削除確認: キャンセル | `usersApi.deleteUser` が呼ばれない |
| `deleteMutation` 成功 | トーストに「削除しました」 |
| `deleteMutation` 失敗 | トーストに「削除に失敗」 |

---

### 3-17. `src/pages/admin/__tests__/UserFormDialog.test.tsx` （新規）

対象: `src/pages/admin/UserFormDialog.tsx`

`QueryClientProvider` + `MemoryRouter` で包む。MSW で `/api/v1/users/*` の返却値・エラーを制御する。

| ブランチ | テストケース |
|---|---|
| `editingUser: null` | 「ユーザー作成」フォームが表示される |
| `editingUser: User` | 「ユーザー編集」フォームが表示される |
| `useEffect`: `editingUser` 変更 | `updateForm` に既存値が設定される |
| `useEffect`: `editingUser` → null | `createForm` がリセットされる |
| create フォーム: username エラー | エラーメッセージ表示 |
| create フォーム: email エラー | エラーメッセージ表示 |
| create フォーム: password エラー | エラーメッセージ表示 |
| edit フォーム: username エラー | エラーメッセージ表示 |
| edit フォーム: email エラー | エラーメッセージ表示 |
| `createMutation` 成功 | トースト + `onOpenChange(false)` |
| `createMutation` 失敗 | エラートースト |
| `updateMutation` 成功 | トースト + `onOpenChange(false)` |
| `updateMutation` 失敗 | エラートースト |
| `isPending: true` | ボタンに「XX中...」と表示 |
| キャンセルボタン | `onOpenChange(false)` が呼ばれる |

---

### 3-18. `src/App.test.tsx` （新規）

対象: `src/App.tsx`

`QueryClientProvider` で包む（`BrowserRouter` は `App` 内部に含まれるため不要）。
MSW で `/api/v1/auth/me` と `/api/v1/users` の返却値を制御し、認証状態・ロールを切り替える。
`MemoryRouter` は使わず、`App` コンポーネント内の `BrowserRouter` をそのまま使う。
初期 URL は `window.history.pushState` で設定する。

| ブランチ | テストケース |
|---|---|
| `/login` | `LoginPage` が描画される |
| `/403` | `ForbiddenPage` が描画される |
| `/404` | `NotFoundPage` が描画される |
| `/*` (未知パス) | `/404` へリダイレクト |
| 認証済み + `/` | `DashboardPage` が描画される |
| 認証済み + `/profile` | `ProfilePage` が描画される |
| 認証済み + ADMIN ロール + `/admin` | `AdminPage` が描画される |
| 認証済み + MANAGER ロール + `/manager` | `ManagerPage` が描画される |
| 未認証 + `/` | `/login` へリダイレクト |
| 認証済み + USER ロール + `/admin` | `/403` へリダイレクト |

---

## Step 3: 既存テストの補強

### `src/schemas/__tests__/auth.test.ts`

追加ケース:
- `username` が 51 文字以上 → エラー（max バリデーション）

### `src/schemas/__tests__/user.test.ts`

追加ケース:
- `createUserSchema`: `username` が空 → エラー
- `createUserSchema`: `username` が 51 文字超 → エラー
- `createUserSchema`: `email` が空 → エラー
- `createUserSchema`: `email` が 256 文字超 → エラー
- `createUserSchema`: パスワードが 8 文字未満 → エラー
- `createUserSchema`: 英小文字を含まないパスワード → エラー
- `updateUserSchema`: `username` が空 → エラー
- `updateUserSchema`: 無効メールアドレス → エラー

---

## Step 4: テストユーティリティの整備

`src/test/` 配下に共通ユーティリティを作成する。

### `src/test/handlers.ts` / `src/test/server.ts` / `src/test/setup.ts`
Step 1 参照。

### `src/test/mockUser.ts`
```ts
import type { AuthUser, User } from '@/types'

export const mockAuthUser: AuthUser = {
  id: 1,
  username: 'testuser',
  email: 'test@example.com',
  role: 'USER',
}

export const mockAdminUser: AuthUser = { ...mockAuthUser, role: 'ADMIN' }
export const mockManagerUser: AuthUser = { ...mockAuthUser, role: 'MANAGER' }

export const mockUser: User = {
  ...mockAuthUser,
  enabled: true,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
}
```

### `src/test/renderWithProviders.tsx`
```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router'

export function renderWithProviders(ui: React.ReactElement, { initialEntries = ['/'] } = {}) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={initialEntries}>
        {ui}
      </MemoryRouter>
    </QueryClientProvider>
  )
}
```

---

## Step 5: 実装順序

1. **MSW セットアップ** - `pnpm add -D msw`、`handlers.ts`・`server.ts`・`setup.ts` 更新、`mockUser.ts`・`renderWithProviders.tsx` 作成
2. **`lib/axios.test.ts`** - MSW でリクエスト検査、CSRF インターセプターのブランチが核心
3. **`lib/queryClient.test.ts`** - MSW で 401 を返し、リダイレクトロジックを検証
4. **`api/auth.test.ts`** / **`api/users.test.ts`** - MSW 経由で API 関数をテスト（シンプル）
5. **`hooks/useToast.test.ts`** - MSW 不要。reducer ブランチが多い
6. **`hooks/useAuth.test.ts`** - MSW で認証 API をインターセプト
7. **`components/common/ProtectedRoute.test.tsx`** / **`RoleProtectedRoute.test.tsx`** - `vi.mock(useAuth)` のみ
8. **`components/common/Layout.test.tsx`** - `vi.mock(useAuth)`、navItems フィルタリングが複雑
9. **`pages/LoginPage.test.tsx`** - MSW でログイン API のエラーケースを制御
10. **`pages/admin/UserFormDialog.test.tsx`** - MSW で作成/編集 API を制御
11. **`pages/admin/AdminPage.test.tsx`** - MSW + `vi.spyOn(window.confirm)`
12. **`pages/manager/ManagerPage.test.tsx`** - MSW で users API を制御
13. **`pages/DashboardPage.test.tsx`** / **`ProfilePage.test.tsx`** / **`ForbiddenPage.test.tsx`** / **`NotFoundPage.test.tsx`** - `vi.mock(useAuth)` のみ
14. **`App.test.tsx`** - MSW でルーティング統合テスト
15. 既存テストの補強

---

## 新規テストファイル一覧（まとめ）

```
frontend/src/
├── App.test.tsx                                        (新規 / MSW)
├── api/
│   └── __tests__/
│       ├── auth.test.ts                                (新規 / MSW)
│       └── users.test.ts                               (新規 / MSW)
├── components/
│   └── common/
│       └── __tests__/
│           ├── Layout.test.tsx                         (新規 / vi.mock(useAuth))
│           ├── ProtectedRoute.test.tsx                 (新規 / vi.mock(useAuth))
│           └── RoleProtectedRoute.test.tsx             (新規 / vi.mock(useAuth))
├── hooks/
│   └── __tests__/
│       ├── useAuth.test.ts                             (新規 / MSW)
│       └── useToast.test.ts                            (新規 / MSW 不要)
├── lib/
│   └── __tests__/
│       ├── axios.test.ts                               (新規 / MSW)
│       ├── queryClient.test.ts                         (新規 / MSW)
│       └── utils.test.ts                               (既存)
├── pages/
│   ├── __tests__/
│   │   ├── DashboardPage.test.tsx                      (新規 / vi.mock(useAuth))
│   │   ├── ForbiddenPage.test.tsx                      (新規 / MSW 不要)
│   │   ├── LoginPage.test.tsx                          (新規 / MSW)
│   │   ├── NotFoundPage.test.tsx                       (新規 / MSW 不要)
│   │   └── ProfilePage.test.tsx                        (新規 / vi.mock(useAuth))
│   ├── admin/
│   │   └── __tests__/
│   │       ├── AdminPage.test.tsx                      (新規 / MSW)
│   │       └── UserFormDialog.test.tsx                 (新規 / MSW)
│   └── manager/
│       └── __tests__/
│           └── ManagerPage.test.tsx                    (新規 / MSW)
├── schemas/
│   └── __tests__/
│       ├── auth.test.ts                                (既存・補強)
│       └── user.test.ts                                (既存・補強)
└── test/
    ├── handlers.ts                                     (新規)
    ├── mockUser.ts                                     (新規)
    ├── renderWithProviders.tsx                         (新規)
    ├── server.ts                                       (新規)
    └── setup.ts                                        (既存・更新)
```

---

## モック戦略の境界

| 対象 | 戦略 | 理由 |
|---|---|---|
| `api/`, `lib/axios`, `lib/queryClient`, `hooks/useAuth`, `pages/LoginPage`, `pages/admin/*`, `pages/manager/*`, `App` | **MSW** | axios インターセプターも含めた実際の HTTP スタックを通して検証できる |
| `components/common/*`, `pages/DashboardPage`, `pages/ProfilePage` | **`vi.mock('@/hooks/useAuth')`** | UI ロジックのみテストしたいため、API 呼び出しは不要 |
| `pages/ForbiddenPage`, `pages/NotFoundPage` | モック不要 | 静的な表示のみ |

---

## 注意事項

- **MSW の `onUnhandledRequest: 'error'`**: ハンドラーに定義されていないリクエストはエラーになる。テストごとに `server.use(...)` で上書きする。
- **`useToast` のグローバル状態**: `listeners` 配列と `memoryState` はモジュールレベルの変数のため、テスト間で状態が共有される。`afterEach` で状態をクリアする処理（REMOVE_TOAST を全件ディスパッチ等）が必要。
- **`window.confirm` のモック**: `AdminPage` の削除確認ダイアログは `vi.spyOn(window, 'confirm')` でモックする。
- **`window.location.href` のモック**: `queryClient.ts` のリダイレクトテストは `Object.defineProperty(window, 'location', ...)` でモックする。
- **`@tanstack/react-query` の `QueryClient`**: テストごとに新しいインスタンスを作成してキャッシュ汚染を防ぐ。`renderWithProviders` でラップして使い回す。

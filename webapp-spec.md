# Web アプリケーション仕様書

## 目次

1. [概要](#1-概要)
2. [技術スタック](#2-技術スタック)
3. [アーキテクチャ](#3-アーキテクチャ)
4. [フロントエンド仕様](#4-フロントエンド仕様)
5. [バックエンド仕様](#5-バックエンド仕様)
6. [データベース仕様](#6-データベース仕様)
7. [認証・認可](#7-認証認可)
8. [セキュリティ対策](#8-セキュリティ対策)
9. [開発環境](#9-開発環境)
10. [テスト戦略](#10-テスト戦略)

---

## 1. 概要

本仕様書は、Vite + React + TypeScript によるフロントエンド SPA と、Java + Spring Boot による RESTful API サーバーで構成される Web アプリケーションの汎用テンプレートを定義する。特定のドメインに依存しない共通技術基盤として使用することを想定している。

本リポジトリには、テンプレート機能に加えて「オンラインバッチ機構」のデモを含む。オンラインバッチデモでは、画面からジョブを受け付け、バックエンドで非同期に逐次処理し、その進捗を API 経由で監視する一連の流れを確認できる。

### 1.1 システム構成

- **フロントエンド**: SPA (Single Page Application)
- **バックエンド**: RESTful API サーバー
- **データベース**: PostgreSQL
- **認証方式**: サーバーサイドセッション + Cookie

---

## 2. 技術スタック

### 2.1 フロントエンド

| 技術 | バージョン | 用途 |
|---|---|---|
| Node.js | 24.x (LTS) | ランタイム・ビルド環境 |
| pnpm | 10.x | パッケージマネージャー |
| TypeScript | 5.x | 言語 |
| React | 19.x | UI フレームワーク |
| Vite | 7.x | ビルドツール・開発サーバー |
| shadcn/ui | latest | UI コンポーネントライブラリ |
| Tailwind CSS | 4.x | スタイリング (shadcn/ui 依存) |
| React Router | 7.x | クライアントサイドルーティング |
| TanStack Query | 5.x | サーバー状態管理・API 通信 |
| Zod | 4.x | スキーマバリデーション |
| React Hook Form | 7.x | フォーム管理 |
| Vitest | 4.x | フロントエンド ユニットテスト |
| React Testing Library | 16.x | コンポーネントテスト |
| Playwright | 1.58.x | E2E テスト |

### 2.2 バックエンド

| 技術 | バージョン | 用途 |
|---|---|---|
| Java | 25 (LTS) | 言語 |
| Spring Boot | 4.0.x | フレームワーク |
| Spring Security | 7.x | 認証・認可 |
| MyBatis Spring Boot Starter | 4.0.x | O/R マッパー (リポジトリ層) |
| Flyway | (Spring Boot BOM 管理) | DB マイグレーション |
| Gradle | 9.3.x (Wrapper) | ビルドツール |
| JUnit 6 | 6.0.x | ユニットテスト |
| Mockito | 5.x | モックライブラリ |
| Testcontainers | 2.0.x | DB 統合テスト |
| JaCoCo | 0.8.x | C1 カバレッジ計測 |
| Bucket4j | 8.x | レート制限 |
| Bouncy Castle | 1.x | Argon2 パスワードハッシュ |

### 2.3 インフラ・ミドルウェア

| 技術 | バージョン | 用途 |
|---|---|---|
| PostgreSQL | 18.x | データベース |
| Docker | 29.x | 開発環境コンテナ管理 |

---

## 3. アーキテクチャ

### 3.1 全体構成

```
[ブラウザ]
    │
    │  HTTP / HTTPS
    ▼
[フロントエンド SPA]  ← Vite Dev Server (開発時) / 静的ファイル配信 (本番時)
    │
    │  REST API (JSON)
    ▼
[Spring Boot API サーバー]
    │
    │  JDBC
    ▼
[PostgreSQL]
```

### 3.2 バックエンド レイヤー構成

```
Controller 層    HTTP リクエスト/レスポンス処理、入力バリデーション
    │
Service 層       ビジネスロジック
    │
Repository 層    DB アクセス (MyBatis Mapper)
    │
Database         PostgreSQL
```

### 3.3 パッケージ構成 (バックエンド)

```
com.example.app
├── config/             設定クラス (Security, MyBatis 等)
├── controller/         REST コントローラー
├── service/            サービスインターフェース
│   └── impl/           サービス実装クラス
├── repository/         MyBatis Mapper インターフェース
├── model/
│   ├── entity/         DB エンティティ (POJO)
│   ├── dto/            リクエスト / レスポンス DTO
│   └── enums/          列挙型
├── security/           Spring Security カスタムクラス
├── exception/          例外クラス・グローバル例外ハンドラー
└── util/               ユーティリティクラス
```

### 3.4 ディレクトリ構成 (フロントエンド)

```
src/
├── components/
│   ├── ui/             shadcn/ui コンポーネント (CLI 生成)
│   └── common/         共通コンポーネント
├── pages/              ページコンポーネント
├── hooks/              カスタムフック
├── lib/                ユーティリティ・設定 (axios インスタンス等)
├── api/                API クライアント関数
├── types/              型定義
└── schemas/            Zod バリデーションスキーマ
```

---

## 4. フロントエンド仕様

### 4.1 主要ページ構成 (テンプレート)

| パス | ページ名 | 認証要否 | 必要ロール |
|---|---|---|---|
| `/login` | ログイン画面 | 不要 | — |
| `/` | ダッシュボード | 要 | 全ロール |
| `/batch-demo` | オンラインバッチデモ | 要 | 全ロール |
| `/profile` | プロフィール | 要 | 全ロール |
| `/manager/*` | マネージャー画面 | 要 | ADMIN, MANAGER |
| `/admin/*` | 管理者画面 | 要 | ADMIN |

### 4.1.1 オンラインバッチデモ画面

- ダッシュボードまたはサイドバーの `オンラインバッチ` から遷移する
- ジョブ名、処理件数、処理間隔、失敗させる件番を入力してジョブを起動する
- 起動後はジョブ一覧に追加され、選択したジョブの進捗率、成功件数、失敗件数、処理イベントを確認できる
- `ACCEPTED` または `RUNNING` のジョブが存在する間、フロントエンドは 1 秒間隔でジョブ一覧・詳細をポーリングする
- `失敗させる件番` を指定した場合、その件の処理で `FAILED` に遷移し、指定しない場合は全件処理後に `COMPLETED` になる

### 4.2 状態管理方針

| 対象 | ライブラリ | 備考 |
|---|---|---|
| サーバー状態 | TanStack Query | フェッチ・キャッシュ・再検証を管理 |
| フォーム状態 | React Hook Form + Zod | バリデーションは Zod スキーマで定義 |
| クライアント状態 | `useState` / `useContext` | 複雑化した場合は Zustand を検討 |

### 4.3 API 通信

- `axios` を TanStack Query のクエリ関数として使用
- CSRF 対策のため、Cookie から取得したトークンを `X-CSRF-TOKEN` リクエストヘッダーに付与
- 401 レスポンス時は TanStack Query のグローバルエラーハンドラーで `/login` へリダイレクト
- エラーレスポンスは `axios` インターセプターで共通処理

### 4.4 ルーティング

- React Router v7 で定義
- `ProtectedRoute` コンポーネントで未認証ユーザーを `/login` にリダイレクト
- ロールベースの `RoleProtectedRoute` コンポーネントで権限不足時に 403 画面を表示

### 4.5 スタイリング

- shadcn/ui コンポーネントをベースとし、Tailwind CSS でカスタマイズ
- レスポンシブデザイン対応 (モバイルファースト)
- ダークモード対応 (shadcn/ui `ThemeProvider` を使用)

---

## 5. バックエンド仕様

### 5.1 API 設計方針

- RESTful 設計
- リクエスト / レスポンスは JSON 形式
- API パスプレフィックス: `/api/v1/`

URL パターン:

```
/api/v1/{resource}                     コレクション操作
/api/v1/{resource}/{id}                単一リソース操作
/api/v1/{resource}/{id}/{sub-resource} サブリソース操作
```

### 5.2 共通レスポンス形式

**成功レスポンス (データあり)**

```json
{
  "data": { "..." : "..." }
}
```

**成功レスポンス (一覧)**

```json
{
  "data": [ { "..." : "..." } ],
  "pagination": {
    "page": 1,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

**エラーレスポンス**

```json
{
  "code": "VALIDATION_ERROR",
  "message": "入力内容に誤りがあります",
  "details": [
    { "field": "email", "message": "メールアドレスの形式が正しくありません" }
  ],
  "timestamp": "2025-01-01T00:00:00Z"
}
```

### 5.3 HTTP ステータスコード

| ステータス | 用途 |
|---|---|
| 200 OK | 取得・更新成功 |
| 201 Created | 新規作成成功 |
| 204 No Content | 削除成功 |
| 400 Bad Request | バリデーションエラー・不正なリクエスト |
| 401 Unauthorized | 未認証 |
| 403 Forbidden | 権限不足 |
| 404 Not Found | リソース未存在 |
| 409 Conflict | 重複リソース |
| 500 Internal Server Error | サーバー内部エラー |

### 5.4 バリデーション

- リクエスト DTO に `@Valid` + Jakarta Bean Validation アノテーションを付与
- `@RestControllerAdvice` + `@ExceptionHandler` でバリデーション例外を共通エラーフォーマットに変換

### 5.5 例外ハンドリング

- 業務例外は独自の `AppException` クラスで表現し、エラーコードとメッセージを保持
- 全ての未ハンドル例外は `GlobalExceptionHandler` で捕捉し、スタックトレースをレスポンスに含めない
- 500 エラー時はサーバー側ログに詳細を記録

### 5.6 ログ

- SLF4J + Logback を使用
- ログレベル: `DEBUG` (dev プロファイル) / `INFO` (prod プロファイル)
- `HandlerInterceptor` でリクエスト・レスポンスのログを記録 (パスワード等の機密情報はマスク)

### 5.7 オンラインバッチデモ API

デモ用のオンラインバッチは PostgreSQL の `online_batch_jobs` テーブルで状態管理する。ジョブ受付時にレコードを作成し、非同期処理の進行に応じて同一レコードを更新する。分散実行やワーカープール制御までは含めず、単一 Spring Boot プロセス内で非同期処理を実行する。

ジョブ状態:

- `ACCEPTED`: 受け付け直後
- `RUNNING`: 非同期処理中
- `COMPLETED`: 全件成功
- `FAILED`: 指定件で失敗、または処理中断

エンドポイント:

| メソッド | パス | 認証要否 | 説明 |
|---|---|---|---|
| GET | `/api/v1/online-batch-jobs` | 要 | ジョブ一覧取得 |
| GET | `/api/v1/online-batch-jobs/{id}` | 要 | ジョブ詳細取得 |
| POST | `/api/v1/online-batch-jobs` | 要 | ジョブ起動 |

起動リクエスト例:

```json
{
  "jobName": "売上CSV取込デモ",
  "totalItems": 8,
  "failureAtItem": null,
  "processingDelayMs": 400
}
```

ジョブ詳細レスポンス例:

```json
{
  "data": {
    "id": 1,
    "jobName": "売上CSV取込デモ",
    "status": "RUNNING",
    "totalItems": 8,
    "processedItems": 3,
    "successCount": 3,
    "failureCount": 0,
    "progressPercent": 37,
    "failureAtItem": null,
    "processingDelayMs": 400,
    "currentItem": "データ4/8",
    "createdAt": "2026-04-25T10:00:00",
    "startedAt": "2026-04-25T10:00:01",
    "completedAt": null,
    "recentEvents": [
      "2026-04-25T10:00:03 データ3/8 の処理が完了しました",
      "2026-04-25T10:00:02 データ3/8 の処理を開始しました"
    ]
  }
}
```

---

## 6. データベース仕様

### 6.1 スキーマ管理 (Flyway)

| 項目 | 設定値 |
|---|---|
| マイグレーションファイル配置 | `src/main/resources/db/migration/` |
| 命名規則 | `V{version}__{description}.sql` |
| 初期データファイル配置 | `src/main/resources/db/testdata/` |
| 初期データ適用条件 | Spring Profile `dev` 時のみ |

命名例:

```
V1__create_users_table.sql
V2__add_index_users_email.sql
```

### 6.2 主要テーブル定義 (テンプレート)

#### users テーブル

```sql
CREATE TABLE users (
    id          BIGSERIAL    PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,          -- Argon2id ハッシュ
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  users             IS 'ユーザー';
COMMENT ON COLUMN users.password    IS 'Argon2id ハッシュ済みパスワード';
COMMENT ON COLUMN users.role        IS 'ロール: ADMIN / MANAGER / USER';
COMMENT ON COLUMN users.enabled     IS 'アカウント有効フラグ';
```

### 6.3 MyBatis 設定

| 項目 | 設定値 |
|---|---|
| Mapper インターフェース配置 | `com.example.app.repository` パッケージ |
| SQL XML ファイル配置 | `src/main/resources/mapper/` |
| @MapperScan 対象 | `com.example.app.repository` |
| mapUnderscoreToCamelCase | `true` (スネークケース ↔ キャメルケース 自動変換) |

### 6.4 命名規則

| 対象 | 規則 | 例 |
|---|---|---|
| テーブル名 | スネークケース・複数形 | `users`, `order_items` |
| カラム名 | スネークケース | `created_at`, `user_id` |
| Java エンティティ | パスカルケース | `User`, `OrderItem` |
| Java フィールド | キャメルケース | `createdAt`, `userId` |

---

## 7. 認証・認可

### 7.1 認証フロー

```
1. POST /api/v1/auth/login  (username + password)
2. Spring Security で UserDetailsService を介して DB 照合
3. Argon2id でパスワードハッシュ検証
4. 認証成功 → HttpSession を生成 (インメモリ管理)
5. Set-Cookie: JSESSIONID=<session-id>; HttpOnly; Secure; SameSite=Lax
6. 以降のリクエストは Cookie でセッション ID を送信
```

### 7.2 セッション設定

| 項目 | 設定値 |
|---|---|
| セッション格納先 | インメモリ (Servlet コンテナの HttpSession) |
| Cookie 名 | `JSESSIONID` |
| HttpOnly | `true` |
| Secure | `true` (HTTPS 環境) |
| SameSite | `Lax` |
| タイムアウト | 30 分 (アイドルタイム) |
| セッション固定攻撃対策 | ログイン後に `sessionFixation().newSession()` でセッション ID 再生成 |

### 7.3 パスワード管理

| 項目 | 設定値 |
|---|---|
| ハッシュアルゴリズム | Argon2id (`Argon2PasswordEncoder`) |
| saltLength | 16 バイト |
| hashLength | 32 バイト |
| parallelism | 1 |
| memory | 65536 (64 MiB) |
| iterations | 3 |
| 最小文字数 | 8 文字 |
| 複雑性要件 | 英大文字・英小文字・数字・記号をそれぞれ 1 文字以上含む (推奨) |

### 7.4 ロール定義

| ロール | 説明 |
|---|---|
| `ADMIN` | 管理者。全機能・全ユーザーデータへのアクセス権を持つ |
| `MANAGER` | マネージャー。一般ユーザー管理等の限定的な管理権限を持つ |
| `USER` | 一般ユーザー。自身のデータへの読み書きのみ可能 |

### 7.5 認可方針

- URL レベルの認可: `SecurityFilterChain` の `authorizeHttpRequests` で定義
- メソッドレベルの認可: `@PreAuthorize` アノテーションで細粒度の制御

```java
// 例
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/users/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) { ... }
```

### 7.6 認証・認可 API エンドポイント

| メソッド | パス | 認証要否 | 説明 |
|---|---|---|---|
| POST | `/api/v1/auth/login` | 不要 | ログイン |
| POST | `/api/v1/auth/logout` | 要 | ログアウト (セッション無効化) |
| GET | `/api/v1/auth/me` | 要 | ログイン中ユーザー情報取得 |

### 7.7 CSRF 対策

- Spring Security の CSRF 保護を有効化
- `CookieCsrfTokenRepository.withHttpOnlyFalse()` でトークンを Cookie 経由で提供
- フロントエンドは Cookie から CSRF トークンを読み取り、`X-CSRF-TOKEN` リクエストヘッダーに付与
- GET / HEAD / OPTIONS / TRACE リクエストは CSRF 検証対象外 (Spring Security デフォルト)

### 7.8 アカウントロック

- ログイン失敗 10 回連続でアカウントを一時ロック
- ロック解除: 一定時間経過後に自動解除 (デフォルト 30 分)、または管理者による手動解除
- `users` テーブルに `failed_login_attempts` / `locked_until` カラムを追加して管理

---

## 8. セキュリティ対策

OWASP Top 10 および OWASP Application Security Verification Standard (ASVS) に基づき対策を実施する。

### 8.1 インジェクション対策

- **SQL インジェクション**: MyBatis はプリペアドステートメントを使用。動的 SQL の `${}` (文字列結合) 記法は原則禁止とし、`#{}` (プレースホルダー) のみ使用
- **XSS**: Jackson のデフォルト設定で JSON シリアライズ時にエスケープ処理済み。React の JSX は変数展開時に自動エスケープ

### 8.2 セキュリティレスポンスヘッダー

Spring Security の `headers()` 設定で以下のヘッダーを全レスポンスに付与する。

| ヘッダー | 設定値 | 目的 |
|---|---|---|
| `X-Content-Type-Options` | `nosniff` | MIME タイプスニッフィング防止 |
| `X-Frame-Options` | `DENY` | クリックジャッキング防止 |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | HTTPS 強制 |
| `Content-Security-Policy` | ※ アプリ要件に合わせて定義 | XSS・コード注入防止 |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | リファラー情報の制御 |
| `Permissions-Policy` | 使用しないブラウザ機能を無効化 | ブラウザ API の権限制限 |

### 8.3 入力バリデーション

- サーバーサイドで全ての入力を Bean Validation で検証 (クライアントサイドのみへの依存禁止)
- 文字列長・形式・許容値の範囲をアノテーションで明示的に定義
- ファイルアップロードを実装する場合は拡張子・MIME タイプ・ファイルサイズを検証

### 8.4 機密情報管理

- パスワード・トークン等をアプリケーションログに出力しない (`@ToString.Exclude` 等で除外)
- DB 接続情報・シークレットは環境変数で管理し、ソースコードにハードコードしない
- エラーレスポンスにスタックトレース・内部実装情報を含めない

### 8.5 レート制限

- ログインエンドポイント (`/api/v1/auth/login`) に対してレート制限を設ける
- 実装: Bucket4j + Spring の `HandlerInterceptor` を使用
- 同一 IP アドレスからのリクエストを一定時間内に制限 (例: 10 回 / 分)

### 8.6 依存ライブラリの脆弱性管理

- バックエンド: OWASP Dependency-Check Gradle プラグインを CI で実行
- フロントエンド: `pnpm audit` を CI で実行

---

## 9. 開発環境

### 9.1 前提ソフトウェア

| ソフトウェア | バージョン |
|---|---|
| Java (JDK) | 25 (LTS) |
| Gradle | Gradle Wrapper を使用 (gradlew コマンド) |
| Node.js | 24.x (LTS) |
| pnpm | 10.x |
| Docker | 29.x |

### 9.2 コンテナ構成 (`compose.yml`)

```yaml
version: "3.8"

services:
  postgres:
    image: postgres:18
    environment:
      POSTGRES_DB: appdb
      POSTGRES_USER: appuser
      POSTGRES_PASSWORD: apppassword
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U appuser -d appdb"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

### 9.3 Spring Profile

| プロファイル | 用途 | 備考 |
|---|---|---|
| `dev` | 開発環境 (デフォルト) | 詳細ログ有効、初期データ (Flyway testdata) を投入 |
| `test` | テスト実行時 | Testcontainers で PostgreSQL コンテナを起動 |

### 9.4 環境変数 (バックエンド)

| 変数名 | デフォルト値 (dev) | 説明 |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/appdb` | DB 接続 URL |
| `DB_USERNAME` | `appuser` | DB ユーザー名 |
| `DB_PASSWORD` | `apppassword` | DB パスワード |
| `SESSION_TIMEOUT` | `1800` | セッションタイムアウト秒数 |

### 9.5 起動手順

```bash
# 1. DB コンテナ起動
docker compose up -d

# 2. バックエンド起動 (Flyway マイグレーション + 初期データ投入が自動実行)
cd backend
./gradlew bootRun --args='--spring.profiles.active=dev'

# 3. フロントエンド起動
cd ../frontend
pnpm install
pnpm dev
```

起動後のアクセス先:

- フロントエンド: `http://localhost:5173`
- バックエンド API: `http://localhost:8080/api/v1`

オンラインバッチデモ確認手順:

1. ブラウザで `http://localhost:5173` を開き、ログインする
2. ダッシュボードの `デモを開く` またはサイドバーの `オンラインバッチ` から `/batch-demo` へ遷移する
3. `ジョブ名`、`処理件数`、`処理間隔(ms)` を設定し、必要であれば `失敗させる件番` を入力する
4. `バッチを起動` を押し、ジョブ一覧とジョブ詳細でステータス遷移とイベントログを確認する

補足:

- バックエンドのジョブ情報は `online_batch_jobs` テーブルに保持されるため、Spring Boot を再起動しても残る
- `失敗させる件番` を空にすると正常完了デモ、`1` 以上を入れると異常終了デモを確認できる

### 9.6 pnpm 設定 (`pnpm-workspace.yaml`)

サプライチェーン攻撃対策として、`minimumReleaseAge` を設定し、公開直後のパッケージバージョンをインストール対象から除外する。

```yaml
# pnpm-workspace.yaml
ignoredBuiltDependencies:
  - esbuild

minimumReleaseAge: 10080
```

### 9.7 フロントエンド開発時の API プロキシ設定

Vite の `vite.config.ts` でバックエンドへのプロキシを設定し、CORS 問題を回避する。

```typescript
// vite.config.ts
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

---

## 10. テスト戦略

### 10.1 テスト方針

| 項目 | 内容 |
|---|---|
| カバレッジ種別 | C1 (分岐カバレッジ) |
| カバレッジ目標 | 100% |
| 計測ツール | JaCoCo |
| レポート出力先 | `build/reports/jacoco/test/html/index.html` |

```bash
# カバレッジレポート生成
./gradlew test jacocoTestReport

# カバレッジ基準未達の場合にビルド失敗
./gradlew jacocoTestCoverageVerification
```

### 10.2 バックエンド テスト構成

```
src/test/java/com/example/app/
├── controller/     @WebMvcTest によるコントローラー スライステスト
├── service/        Mockito によるユニットテスト
├── repository/     Testcontainers を用いた MyBatis Mapper 統合テスト
└── util/           ユーティリティクラスのユニットテスト
```

#### Controller テスト (`@WebMvcTest`)

```java
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserService userService;

    @Test
    void getUser_returnsUser() throws Exception {
        // ...
    }
}
```

#### Service テスト (Mockito)

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserServiceImpl userService;

    @Test
    void findById_returnsUser() {
        // ...
    }
}
```

#### Repository テスト (Testcontainers)

> **注意**: Spring Boot 4 で `@MyBatisTest` が削除された `FlywayAutoConfiguration` を参照するため使用不可。
> `@SpringBootTest(webEnvironment = NONE)` + Testcontainers で代替する。

```java
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UserRepositoryTest {

    @Container
    static PostgreSQLContainer postgres =
        new PostgreSQLContainer("postgres:18");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    UserRepository userRepository;

    @Test
    void findById_returnsUser() {
        // ...
    }
}
```

### 10.3 フロントエンド テスト構成

| テスト種別 | ツール | 対象 |
|---|---|---|
| ユニットテスト | Vitest | カスタムフック、ユーティリティ関数、バリデーションスキーマ |
| コンポーネントテスト | React Testing Library | 重要 UI コンポーネントのインタラクション確認 |
| E2E テスト | Playwright | 主要ユーザーシナリオのフルフロー検証 |

### 10.4 E2E テスト (Playwright)

```
e2e/
├── auth/
│   ├── login.spec.ts           ログイン正常・異常系
│   └── logout.spec.ts          ログアウト
├── user/
│   └── user-management.spec.ts ユーザー CRUD (管理者操作)
└── fixtures/
    └── auth.ts                 認証ヘルパー (ログイン済み状態のセットアップ)
```

Playwright 設定:

```typescript
// playwright.config.ts
export default defineConfig({
  testDir: './e2e',
  use: {
    baseURL: 'http://localhost:5173',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox',  use: { ...devices['Desktop Firefox'] } },
    { name: 'webkit',   use: { ...devices['Desktop Safari'] } },
  ],
})
```

### 10.5 CI でのテスト実行

```bash
# バックエンド: ユニット + 統合テスト + カバレッジレポート生成
./gradlew test jacocoTestReport jacocoTestCoverageVerification

# フロントエンド: ユニット + コンポーネントテスト
pnpm test

# E2E テスト (フロントエンド・バックエンド起動後)
pnpm exec playwright test
```

---

## Appendix A: テンプレート API エンドポイント一覧

| メソッド | パス | 認証 | ロール | 説明 |
|---|---|---|---|---|
| POST | `/api/v1/auth/login` | 不要 | — | ログイン |
| POST | `/api/v1/auth/logout` | 要 | 全ロール | ログアウト |
| GET | `/api/v1/auth/me` | 要 | 全ロール | 自ユーザー情報取得 |
| GET | `/api/v1/online-batch-jobs` | 要 | 全ロール | オンラインバッチジョブ一覧取得 |
| GET | `/api/v1/online-batch-jobs/{id}` | 要 | 全ロール | オンラインバッチジョブ詳細取得 |
| POST | `/api/v1/online-batch-jobs` | 要 | 全ロール | オンラインバッチジョブ起動 |
| GET | `/api/v1/users` | 要 | ADMIN, MANAGER | ユーザー一覧取得 |
| GET | `/api/v1/users/{id}` | 要 | ADMIN, MANAGER | ユーザー詳細取得 |
| POST | `/api/v1/users` | 要 | ADMIN | ユーザー作成 |
| PUT | `/api/v1/users/{id}` | 要 | ADMIN | ユーザー更新 |
| DELETE | `/api/v1/users/{id}` | 要 | ADMIN | ユーザー削除 |

## Appendix B: Gradle 設定概要 (`build.gradle`)

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.3'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'jacoco'
    id 'org.owasp.dependencycheck' version '12.2.0'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

jacoco {
    toolVersion = "0.8.14"
}

jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
    dependsOn test
}

jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = 'BRANCH'
                value = 'COVEREDRATIO'
                minimum = 1.0  // C1: 分岐カバレッジ 100%
            }
        }
    }
    dependsOn jacocoTestReport
}

test {
    useJUnitPlatform()
    finalizedBy jacocoTestReport
}
```

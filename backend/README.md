# Backend

Spring Boot + MyBatis + PostgreSQL による RESTful API サーバー。

## 前提

| ソフトウェア | バージョン |
|---|---|
| Java (JDK) | 25 |
| Podman | 5.x |
| Podman Compose | 1.x |

Gradle は Wrapper (`gradlew`) を使うためインストール不要。

---

## 起動手順

### 1. DB コンテナを起動

```bash
cd ..   # リポジトリルート (podman-compose.yml のある場所)
podman-compose up -d
```

ヘルスチェックが `healthy` になるまで待つ。

```bash
podman ps   # STATUS 列が "healthy" であることを確認
```

### 2. バックエンドを起動

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=dev'
```

初回起動時に Flyway が自動でマイグレーションと開発用初期データの投入を行う。

起動後、`http://localhost:8080` でリクエストを受け付ける。

---

## 動作確認 (curl)

### ログイン

```bash
curl -c cookies.txt -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Password1!"}'
```

> 注意: 開発用初期データのパスワードハッシュはダミー値のため、実際にログインするには
> DB のハッシュを正しい Argon2id 値に置き換えるか、別途ユーザーを作成してください。

### ログイン中ユーザー情報取得

```bash
# CSRF トークンを Cookie から取得してヘッダーに付与する
CSRF=$(grep XSRF-TOKEN cookies.txt | awk '{print $7}')
curl -b cookies.txt -X GET http://localhost:8080/api/v1/auth/me \
  -H "X-XSRF-TOKEN: $CSRF"
```

### ユーザー一覧取得 (ADMIN / MANAGER のみ)

```bash
curl -b cookies.txt -X GET "http://localhost:8080/api/v1/users?page=1&size=20" \
  -H "X-XSRF-TOKEN: $CSRF"
```

### ログアウト

```bash
curl -b cookies.txt -X POST http://localhost:8080/api/v1/logout \
  -H "X-XSRF-TOKEN: $CSRF"
```

---

## テスト

### ユニットテスト + 統合テスト

```bash
./gradlew test
```

- Controller テスト: `@WebMvcTest` (MockMvc)
- Service テスト: Mockito
- Repository テスト: Testcontainers (PostgreSQL コンテナを自動起動)

### カバレッジレポート生成

```bash
./gradlew test jacocoTestReport
```

レポートは `build/reports/jacoco/test/html/index.html` で確認できる。

### カバレッジ基準チェック (C1: 分岐カバレッジ 100%)

```bash
./gradlew jacocoTestCoverageVerification
```

基準未達の場合はビルドが失敗する。

### 脆弱性スキャン

事前に [NVD](https://nvd.nist.gov/developers/request-an-api-key) でAPIキーを申請し、発行された値を環境変数にセットする。

```bash
export NVD_API_KEY={NVD APIキー}
```

```bash
./gradlew dependencyCheckAnalyze
```

レポートは `build/reports/dependency-check-report.html`。

---

## 環境変数

デフォルト値は `dev` プロファイルで動作するよう設定済み。本番環境では必ず変更すること。

| 変数名 | デフォルト値 | 説明 |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/appdb` | DB 接続 URL |
| `DB_USERNAME` | `appuser` | DB ユーザー名 |
| `DB_PASSWORD` | `apppassword` | DB パスワード |
| `SESSION_TIMEOUT` | `1800` | セッションタイムアウト (秒) |

---

## Spring Profile

| プロファイル | 用途 |
|---|---|
| `dev` (デフォルト) | 詳細ログ有効、`db/testdata/` の初期データも投入 |
| `test` | テスト実行時に自動適用 (Testcontainers 使用) |

---

## 依存関係管理 (Dependency Locking / Version Catalog / Renovate)

- Gradle dependency locking を有効化しています。依存更新後は lockfile を更新してください。
- lock 更新コマンド:

```bash
./gradlew dependencies --write-locks
```

- Version Catalog は `gradle/libs.versions.toml` で管理しています。
- BOM は Version Catalog 経由で選択できます（例: `libs.spring.boot.bom.v4003` / `libs.spring.boot.bom.v4002`、`libs.testcontainers.bom.v20` / `libs.testcontainers.bom.v19`）。
- Renovate はリポジトリルートの `renovate.json` で管理し、Gradle 関連更新には `minimumReleaseAge: 7 days` を適用しています。

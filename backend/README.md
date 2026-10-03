# Backend

Spring Boot + MyBatis + PostgreSQL による RESTful API サーバー。

Rust 版との共通契約は、品質レビューでセッション・並行更新・バッチ復旧・入力検証・画面のエラー処理を修正している。レビューと検証の記録は `rust-react-demo/codex-review.md` にまとめている。

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

## テスト・品質チェック

Rust 版との HTTP 差分は、両版をそれぞれ新しい DB で起動してから Rust 側の `backend/` で実行する。

差分用の Java サーバーは、このリポジトリの `backend/` から起動する:

```bash
TRUSTED_PROXY_COUNT=1 ./gradlew bootRun --args='--spring.profiles.active=dev'
```

Rustサーバーも独立した新しいDBと `TRUSTED_PROXY_COUNT=1` を指定して8081で起動する。通常の起動手順では信頼プロキシを有効にする必要はない。

```bash
COMPAT_JAVA_URL=http://localhost:8080 COMPAT_RUST_URL=http://localhost:8081 \
  cargo test --locked --test compat -- --ignored --nocapture
```

明示実行時は両 URL が必須。差分シナリオではレート制限をX-Forwarded-Forで分離するため、両テストサーバーを `TRUSTED_PROXY_COUNT=1` で起動する（アプリの既定値は0）。

**マージ順序**: Rust リポジトリの `.github/workflows/compat.yaml` は、このリポジトリのリビジョン (既定は `main`) を参照して比較する。したがって共通契約を変える修正は **このリポジトリを先にマージする**こと。逆順にすると Rust 側の比較ジョブが参照先に契約を見つけられず失敗する。先にマージできない場合は、Rust リポジトリのリポジトリ変数 `COMPAT_JAVA_REF` に対象ブランチ名を設定して参照先を切り替える。

2026-09-29のローカル実サーバー比較は、新規の独立DBで150ステップ・差異0件。GitHub上での専用ワークフロー実行は未確認。

### 全品質チェックを一括実行 (CI と同等)

```bash
./gradlew spotlessCheck checkstyleMain checkstyleTest test spotbugsMain jacocoTestCoverageVerification
```

---

### ユニットテスト + 統合テスト + アーキテクチャーテスト

```bash
./gradlew test
```

- Controller テスト: `@WebMvcTest` (MockMvc)
- Service テスト: Mockito
- Repository テスト: Testcontainers (PostgreSQL コンテナを自動起動)
- アーキテクチャーテスト: ArchUnit (レイヤー依存ルール・循環依存禁止等)

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

---

### コードフォーマット (Spotless + palantir-java-format)

フォーマット違反を検出する (CI で実行):

```bash
./gradlew spotlessCheck
```

違反を自動修正する:

```bash
./gradlew spotlessApply
```

---

### 静的解析 (Checkstyle)

命名規則・import ルール・波括弧の必須化等を検査する:

```bash
./gradlew checkstyleMain checkstyleTest
```

レポートは `build/reports/checkstyle/` で確認できる。

### 静的解析 (SpotBugs)

バイトコードレベルのバグパターン (null 参照・リソースリーク等) を検査する:

```bash
./gradlew spotbugsMain
```

レポートは `build/reports/spotbugs/main.html` で確認できる。

---

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

## 運用時の契約

- 認証済み要求では `SessionValidationFilter` が認可前に現在のユーザーを DB から照合する。削除・無効化・有効なロック、ロール・ユーザー名・パスワードの変更を検出すると既存セッションを破棄する。検出は次の要求時で、変更後に要求を送る前に元の状態へ戻した場合は対象外。DB 照合の失敗は 500 になる
- ユーザー更新とログイン失敗の記録は対象行を `FOR UPDATE` でロックし、現在値の読み取りと更新を同じトランザクションで行う
- バッチ処理の例外を FAILED として保存する。失敗記録も DB 障害で保存できなかった場合はログに残し、次回起動時に ACCEPTED / RUNNING を FAILED として回収する。完了時刻・回収イベントを保存し、進捗値と既存の終端状態を保持する。同じ DB を使う API は単一プロセスが前提
- 一覧は `page >= 1`、`1 <= size <= 100`（既定 1 / 20）。offset は long で計算する。不正な値・型変換は 400、認証・認可を通過した未知のパスは 404、非対応 Content-Type は 415

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
- BOM は Version Catalog の `libs.spring.boot.bom` / `libs.testcontainers.bom` で指定し、対応するバージョンは `[versions]` で管理しています。
- Renovate はリポジトリルートの `renovate.json` で管理し、Gradle 関連更新には `minimumReleaseAge: 7 days` を適用しています。
- 2026-10-03 に公開から 7 日以上経過した GA 版へ更新しました。Spring Boot は 4.1.1、MyBatis Starter は 4.1.0 です。Spring Boot の BOM が管理する依存は BOM に合わせます。
- JSON 処理は Jackson 3 (`tools.jackson`) を使用します。JSON 処理失敗は非検査例外の `JacksonException` として伝播します。

## バッチ・セッションの保護設定

オンラインバッチは単一 API プロセスで実行する。`app.batch` の既定値は全体同時4件、
利用者ごと同時2件・60秒間10件。利用者の識別には認証済みアカウントの固定IDを使う。
利用者の上限超過は429、全体の空き枠不足や停止中は503を返す。再試行は利用者が時間を空けて行う。
保存・実行投入に失敗した要求も、取得済みの60秒間の受付枠は消費する。
受付履歴の利用者状態は最大10,000件で、実行中でなく60秒間の記録もない状態を破棄する。

ジョブ履歴は全体1,000件を上限とし、完了・失敗したジョブだけを古い順に削除する。
終了から7日経過した履歴も削除する（毎分および新規受付時）。実行中のジョブは削除しない。
一覧APIは `page`（1以上、既定1）と `size`（1〜100、既定20）を受け取り、
`data` と `pagination` を返す。削除済みジョブの詳細取得は404になる。
各上限は `application.yml` の `app.batch` で設定でき、正数が必須。

再ログインすると、同じアカウントの以前のセッションは次のアクセスで401になる。
新しいログインのセッションはローテーションされ、更新操作に使うCSRF cookieも再発行する。
受付制限・セッション管理・起動時の未完了ジョブ回収は単一プロセスを前提とするため、
APIを複数プロセスへ拡張する前に共有ストアと排他的なジョブ所有権を導入すること。

`compose.yaml` のDB、`compose.prod.yaml` の検査用HTTP/HTTPS、`pod.yaml` のDB公開先は
`127.0.0.1` に限定する。既存コンテナには再作成後に反映される。
検査用の既知パスワード・pepper・自己署名証明書は本番の秘密情報に置き換えること。

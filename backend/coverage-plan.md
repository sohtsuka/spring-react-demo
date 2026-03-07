# C1 (ブランチ) カバレッジ 100% 達成計画

## 現状確認

`build.gradle` にて JaCoCo の `BRANCH` カバレッジ 100% が `jacocoTestCoverageVerification` で検証済み。
現在のテストスイートで未カバーのブランチを以下に列挙し、追加すべきテストを具体的に示す。

---

## 1. `UserServiceImpl`

### 未カバーブランチ

| メソッド | 条件 | 未カバー分岐 |
|---|---|---|
| `update` | `findById` が空 | ユーザー未存在の例外スロー |
| `update` | `request.username() != null` | true 側（非 null username 指定） |
| `update` | `!request.username().equals(user.getUsername())` | false 側（同名） / true 側かつ `existsByUsername` true（重複） |
| `update` | `request.email() != null` | true 側（非 null email 指定） |
| `update` | `!request.email().equals(user.getEmail())` | false 側（同値） / true 側かつ `existsByEmail` true（重複） |
| `update` | `request.enabled() != null` | true 側 |
| `recordLoginFailure` | `findByUsername` が空 | Optional が empty → `ifPresent` に入らない |

### 追加テスト (`UserServiceTest`)

```
update_notFound_throwsAppException
  → findById が empty → USER_NOT_FOUND

update_withSameUsername_skipsCheck
  → request.username() == user.getUsername() → existsByUsername 呼ばれない

update_withNewUsername_duplicate_throwsAppException
  → request.username() != null && 異なる && existsByUsername true → USERNAME_ALREADY_EXISTS

update_withNewUsername_notDuplicate_updatesUsername
  → request.username() != null && 異なる && existsByUsername false → setUsername

update_withSameEmail_skipsCheck
  → request.email() == user.getEmail() → existsByEmail 呼ばれない

update_withNewEmail_duplicate_throwsAppException
  → request.email() != null && 異なる && existsByEmail true → EMAIL_ALREADY_EXISTS

update_withNewEmail_notDuplicate_updatesEmail
  → request.email() != null && 異なる && existsByEmail false → setEmail

update_withEnabledField_setsEnabled
  → request.enabled() != null → setEnabled

recordLoginFailure_userNotFound_doesNothing
  → findByUsername が empty → incrementFailedLoginAttempts 呼ばれない
```

---

## 2. `AuthController`

### 未カバーブランチ

| メソッド | 条件 | 未カバー分岐 |
|---|---|---|
| `login` | `LockedException` catch | アカウントロック時 |
| `login` | `DisabledException` catch | アカウント無効時 |
| `login` | `if (oldSession != null)` | true 側（既存セッションあり） |
| `logout` | `if (session != null)` | true 側（セッションあり）/ false 側（セッションなし） |
| `me` | 全体 | テストなし |

### 追加テスト (`AuthControllerTest`)

```
login_withLockedAccount_returns401
  → authenticate が LockedException をスロー → ACCOUNT_LOCKED (401)

login_withDisabledAccount_returns401
  → authenticate が DisabledException をスロー → ACCOUNT_DISABLED (401)

login_withExistingSession_invalidatesOldSession
  → MockMvc の SessionRepositoryRequestCache 等で既存セッションを持たせた状態でログイン
    → oldSession.invalidate() が呼ばれることを確認
  ※ MockMvcBuilders で HttpSession を事前に作成する方法:
    MockHttpSession を request に設定して perform() に渡す

logout_withSession_invalidatesSession
  → MockHttpSession を渡して POST /logout → セッション無効化確認 (204)

logout_withoutSession_returns204
  → セッションなしで POST /logout → 204 (現在の logout テストがこれに相当するか確認)

me_authenticated_returnsCurrentUser
  → GET /auth/me で Authentication を SecurityContext に設定 → 200 + ユーザー情報
  ※ standaloneSetup では @WithMockUser が使えないため、
    SecurityContextHolder.setContext() で Authentication をセットする
    または MockMvcBuilders に springSecurity() を追加しない方式で
    Principal を直接 controller に渡す方法（perform().principal()）を用いる
```

---

## 3. `PepperPasswordEncoder`（新規テストクラス）

### 未カバーブランチ

| メソッド | 条件 | 未カバー分岐 |
|---|---|---|
| `matches` | `delimIdx < 0` | true（区切り文字なし）/ false（正常フォーマット） |
| `upgradeEncoding` | `delimIdx < 0` | true / false |

### 追加テスト（`PepperPasswordEncoderTest` を新規作成）

```
encode_producesDelimitedFormat
  → encode("password") → "xxxxxx:$argon2id$..." の形式

matches_withValidEncodedPassword_returnsTrue
  → encode した文字列を matches に渡す → true

matches_withWrongPassword_returnsFalse
  → encode("password") の結果に別パスワードを matches → false

matches_withNoDelimiter_returnsFalse
  → delimIdx < 0 の場合 → false（区切り文字を含まない文字列）

upgradeEncoding_withValidFormat_delegatesToInner
  → 正常フォーマット → delegate.upgradeEncoding() の結果を返す

upgradeEncoding_withNoDelimiter_returnsFalse
  → delimIdx < 0 → false
```

---

## 4. `CustomUserDetails`

### 未カバーブランチ

| メソッド | 条件 | 未カバー分岐 |
|---|---|---|
| `isAccountNonLocked` | `lockedUntil == null` | false 側（非 null の場合） |
| `isAccountNonLocked` | `lockedUntil.isBefore(LocalDateTime.now())` | true（過去）/ false（未来 = ロック中） |

### 追加テスト（`CustomUserDetailsTest` を新規作成）

```
isAccountNonLocked_whenLockedUntilIsNull_returnsTrue
  → user.setLockedUntil(null) → true

isAccountNonLocked_whenLockedUntilIsInPast_returnsTrue
  → user.setLockedUntil(LocalDateTime.now().minusMinutes(1)) → true

isAccountNonLocked_whenLockedUntilIsInFuture_returnsFalse
  → user.setLockedUntil(LocalDateTime.now().plusMinutes(30)) → false
```

---

## 5. `CustomUserDetailsService`

### 未カバーブランチ

| メソッド | 条件 | 未カバー分岐 |
|---|---|---|
| `loadUserByUsername` | `orElseThrow` | ユーザー未存在 → UsernameNotFoundException |

### 追加テスト（`CustomUserDetailsServiceTest` を新規作成）

```
loadUserByUsername_existingUser_returnsCustomUserDetails
  → findByUsername が user を返す → CustomUserDetails

loadUserByUsername_notFound_throwsUsernameNotFoundException
  → findByUsername が empty → UsernameNotFoundException
```

---

## 6. `RateLimitInterceptor`

### 未カバーブランチ

| メソッド | 条件 | 未カバー分岐 |
|---|---|---|
| `preHandle` | `bucket.tryConsume(1)` | true（許可）/ false（制限） |
| `resolveClientIp` | `xForwardedFor != null && !isBlank()` | true（ヘッダーあり）/ false（ヘッダーなし） |

### 追加テスト（`RateLimitInterceptorTest` を新規作成）

```
preHandle_whenUnderLimit_returnsTrue
  → 通常リクエスト → true 返却

preHandle_whenOverLimit_returnsFalseAndWrites429
  → Bucket をリフレクションまたは MAX_REQUESTS_PER_MINUTE+1 回呼び出して枯渇させる
    → false 返却 + response status 429 + JSON ボディ

resolveClientIp_withXForwardedFor_returnsFirstIp
  → request に "X-Forwarded-For: 1.2.3.4, 5.6.7.8" を設定 → "1.2.3.4"

resolveClientIp_withBlankXForwardedFor_returnsRemoteAddr
  → X-Forwarded-For が空文字 / null → request.getRemoteAddr()

※ MockHttpServletRequest / MockHttpServletResponse を使用
```

---

## 7. `RequestLoggingInterceptor`

### 未カバーブランチ

| メソッド | 条件 | 未カバー分岐 |
|---|---|---|
| `afterCompletion` | `startTime != null` | true（正常フロー）/ false（属性なし） |

### 追加テスト（`RequestLoggingInterceptorTest` を新規作成）

```
afterCompletion_withStartTime_logsDuration
  → preHandle → afterCompletion の順で呼ぶ → startTime != null 分岐

afterCompletion_withoutStartTime_usesMinusOne
  → preHandle を呼ばずに afterCompletion → startTime == null → duration = -1
```

---

## 8. `SecurityConfig.CsrfCookieFilter`

### 未カバーブランチ

| メソッド | 条件 | 未カバー分岐 |
|---|---|---|
| `doFilterInternal` | `csrfToken != null` | true（トークンあり）/ false（なし） |

### 対応方針

`CsrfCookieFilter` は `private static final class` のため直接インスタンス化できない。
統合テスト（`@SpringBootTest` + `MockMvc`）で CSRF フィルターチェーンを通すことで間接的にカバーする。

```
CsrfCookieFilterIntegrationTest（新規）
  → @SpringBootTest(webEnvironment = RANDOM_PORT) + TestRestTemplate
    または @AutoConfigureMockMvc で MockMvc を使用

csrfCookieFilter_withCsrfToken_writesTokenToResponse
  → 認証済みリクエストで GET /api/v1/users を実行
    → レスポンスの Set-Cookie に XSRF-TOKEN が含まれる（csrfToken != null）

csrfCookieFilter_withoutCsrfAttribute_skipsTokenWrite
  → CSRF 除外パス（/api/v1/auth/login）へのリクエスト
    → csrfToken == null のパスが通る
  ※ただし login は CSRF 除外設定のため CsrfFilter 自体がスキップされる可能性あり。
    別の方法として、フィルターを直接テストするリフレクション or
    SecurityConfig をテスト用に分離する対応も検討する。
```

---

## 9. `GlobalExceptionHandler`

### 未カバーブランチ

| ハンドラー | 未カバー理由 |
|---|---|
| `handleAuthenticationException` | standaloneSetup では Spring Security フィルターが動かないため未到達 |
| `handleAccessDeniedException` | 同上 |
| `handleMessageNotReadable` | 不正フォーマット JSON のテストなし |
| `handleMethodNotSupported` | 未サポート HTTP メソッドのテストなし |
| `handleGeneral` | 予期しない例外を投げるテストなし |
| `handleValidation` の `orElse` | 実質デッドコード（フィールドエラーが必ず存在）、JaCoCo 上で missed になる可能性 |

### 追加テスト（`GlobalExceptionHandlerTest` を新規作成）

```
handleAuthenticationException_returns401
  → GlobalExceptionHandler を直接インスタンス化し handleAuthenticationException() を呼ぶ
    または MockMvc に authenticationException を throw させる

handleAccessDeniedException_returns403
  → 同様に直接呼び出し or MockMvc

handleMessageNotReadable_returns400
  → UserControllerTest で不正 JSON ボディ（"invalid-json"）を送信
    → 400 + VALIDATION_ERROR

handleMethodNotSupported_returns405
  → /api/v1/users に PATCH リクエスト → 405

handleGeneral_returns500
  → UserService.findAll() が RuntimeException を throw するようモックし GET /api/v1/users
    → 500 + INTERNAL_SERVER_ERROR

handleValidation_withMultipleFieldErrors_returnsAllFields
  → 複数フィールドエラーを含むリクエストで distinct + map が全フィールドをカバーするか確認
  ※ orElse("入力値が正しくありません") は実質到達不能コードだが、
    JaCoCo がブランチとして計上する場合は @ExcludeFromJacocoGeneratedCoverage を検討
    または JacocoTestCoverageVerification の excludes に追加する方針で合意を取る
```

---

## 10. 実装の進め方

### フェーズ 1: 純粋ユニットテスト追加（DBなし・Spring Context なし）

優先度高・インフラ不要のため最初に着手する。

1. `UserServiceTest` に `update` 系・`recordLoginFailure` 系テスト追加
2. `AuthControllerTest` に `LockedException`・`DisabledException`・`me`・`logout` テスト追加
3. `PepperPasswordEncoderTest` 新規作成
4. `CustomUserDetailsTest` 新規作成
5. `CustomUserDetailsServiceTest` 新規作成
6. `RateLimitInterceptorTest` 新規作成
7. `RequestLoggingInterceptorTest` 新規作成
8. `GlobalExceptionHandlerTest` 新規作成

### フェーズ 2: 統合テスト追加（Spring Context 必要）

9. `SecurityConfig.CsrfCookieFilter` のカバレッジ（統合テスト）

### フェーズ 3: カバレッジ検証

```bash
DOCKER_HOST=unix:///run/user/1000/podman/podman.sock \
TESTCONTAINERS_RYUK_DISABLED=true \
./gradlew test jacocoTestCoverageVerification
```

---

## 11. JaCoCo 除外対象の検討

以下は実質デッドコードまたはテスト困難なため、除外を検討する。

| クラス/メソッド | 理由 | 対応 |
|---|---|---|
| `GlobalExceptionHandler.handleValidation` の `orElse` | `field` は `getFieldErrors()` 由来のため実質到達不能 | `@Generated` アノテーション or excludes 設定 |
| `Application.main()` | Spring Boot 起動エントリーポイント | JaCoCo 慣例として除外が一般的 |
| `SecurityConfig` のラムダ（entryPoint / accessDenied） | 統合テストでカバー可能だが複雑 | 統合テストでカバー（フェーズ 2） |

除外設定例（`build.gradle`）:
```groovy
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = 'BRANCH'
                value = 'COVEREDRATIO'
                minimum = 1.0
            }
        }
    }
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                'com/example/app/Application.class'
            ])
        }))
    }
    dependsOn jacocoTestReport
}
```

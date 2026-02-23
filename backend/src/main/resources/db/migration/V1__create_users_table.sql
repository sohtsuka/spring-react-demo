CREATE TABLE users (
    id                    BIGSERIAL    PRIMARY KEY,
    username              VARCHAR(50)  NOT NULL UNIQUE,
    email                 VARCHAR(255) NOT NULL UNIQUE,
    password              VARCHAR(255) NOT NULL,
    role                  VARCHAR(20)  NOT NULL DEFAULT 'USER',
    enabled               BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    locked_until          TIMESTAMP,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  users                       IS 'ユーザー';
COMMENT ON COLUMN users.password              IS 'Argon2id ハッシュ済みパスワード';
COMMENT ON COLUMN users.role                  IS 'ロール: ADMIN / MANAGER / USER';
COMMENT ON COLUMN users.enabled               IS 'アカウント有効フラグ';
COMMENT ON COLUMN users.failed_login_attempts IS 'ログイン失敗回数';
COMMENT ON COLUMN users.locked_until          IS 'アカウントロック解除日時 (NULL = ロックなし)';

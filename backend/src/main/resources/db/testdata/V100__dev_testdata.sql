-- 開発環境用初期データ
-- パスワードはすべて "Password1!" (アプリソルト + ペッパー + Argon2id ハッシュ)
-- フォーマット: <Base64-appSalt>:<argon2id-hash>
-- ペッパー: dev-pepper-change-in-production (application-dev.yml)
-- 注意: 本番環境には絶対に適用しないこと (dev プロファイル専用)

INSERT INTO users (username, email, password, role, enabled) VALUES
    ('admin',
     'admin@example.com',
     '2YbisKba0yLS9CIbbG/9yLYNRA2AKbbWGLcYGGzJbKc=:$argon2id$v=19$m=65536,t=3,p=1$SMF6w+Vm/FXGQJAHUZiisg$kctwOpyswYSgNHzkNXWTvPhfh9Ckvb9BCs54ZyusP1I',
     'ADMIN',
     TRUE),
    ('manager',
     'manager@example.com',
     'aACDA3ZgUnZysDUl1Lt/aGALIj1zyE+uHcs1LTdxwag=:$argon2id$v=19$m=65536,t=3,p=1$1knzxsd0mFNo1Fej7ac4IA$gwyFb7UdkfJUTgtQO6WRa1M0fswt5sGcgWyBQ6BjMu0',
     'MANAGER',
     TRUE),
    ('user1',
     'user1@example.com',
     'ZrIjaRRrbHdY1JEaQwszZVHRVuNxvpG8gIlepLzZmhA=:$argon2id$v=19$m=65536,t=3,p=1$L/HAHmq/P1oum3CaZkANOg$3YrqNHO1fX+TaF7G3aAMnuvsmhI1GjVjR7kVID9PrGM',
     'USER',
     TRUE);

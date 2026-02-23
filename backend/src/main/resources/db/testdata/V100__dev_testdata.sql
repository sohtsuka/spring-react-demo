-- 開発環境用初期データ
-- パスワードはすべて "Password1!" (Argon2id ハッシュ)
-- 注意: 本番環境には絶対に適用しないこと (dev プロファイル専用)

INSERT INTO users (username, email, password, role, enabled) VALUES
    ('admin',
     'admin@example.com',
     '$argon2id$v=19$m=65536,t=3,p=1$gNziobEvCHJRiRIwhZF+fA$ZM7nfv5ktfP3x/UFNZrFNFdqo6TzzbtBxFz/okwSgm8',
     'ADMIN',
     TRUE),
    ('manager',
     'manager@example.com',
     '$argon2id$v=19$m=65536,t=3,p=1$gNziobEvCHJRiRIwhZF+fA$ZM7nfv5ktfP3x/UFNZrFNFdqo6TzzbtBxFz/okwSgm8',
     'MANAGER',
     TRUE),
    ('user1',
     'user1@example.com',
     '$argon2id$v=19$m=65536,t=3,p=1$gNziobEvCHJRiRIwhZF+fA$ZM7nfv5ktfP3x/UFNZrFNFdqo6TzzbtBxFz/okwSgm8',
     'USER',
     TRUE);

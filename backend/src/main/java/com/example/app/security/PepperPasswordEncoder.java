package com.example.app.security;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * ソルト＋ペッパーを適用する PasswordEncoder ラッパー。
 *
 * <p><b>多層ハッシュ設計:</b>
 * <ol>
 *   <li>アプリケーションソルト (32 バイト乱数, Base64) を生成してパスワードカラムに保存</li>
 *   <li>ペッパー (環境変数 APP_PEPPER, サーバー秘密鍵) を結合</li>
 *   <li>Argon2id が内部でさらにランダムソルトを付与してハッシュ化</li>
 * </ol>
 *
 * <p><b>保存フォーマット:</b> {@code <Base64-appSalt>:<argon2id-hash>}
 *
 * <p>DB が漏洩してもペッパーがなければ解読不可。
 * ペッパーが漏洩してもアプリソルトにより総当たりコストが増大。
 */
public class PepperPasswordEncoder implements PasswordEncoder {

    private static final int APP_SALT_BYTES = 32;
    private static final String DELIMITER = ":";

    private final PasswordEncoder delegate;
    private final String pepper;
    private final SecureRandom random = new SecureRandom();

    public PepperPasswordEncoder(PasswordEncoder delegate, String pepper) {
        this.delegate = delegate;
        this.pepper = pepper;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        byte[] saltBytes = new byte[APP_SALT_BYTES];
        random.nextBytes(saltBytes);
        String appSalt = Base64.getEncoder().encodeToString(saltBytes);
        String hash = delegate.encode(pepper + appSalt + rawPassword);
        return appSalt + DELIMITER + hash;
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        int delimIdx = encodedPassword.indexOf(DELIMITER);
        if (delimIdx < 0) {
            return false;
        }
        String appSalt = encodedPassword.substring(0, delimIdx);
        String hash    = encodedPassword.substring(delimIdx + 1);
        return delegate.matches(pepper + appSalt + rawPassword, hash);
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        int delimIdx = encodedPassword.indexOf(DELIMITER);
        if (delimIdx < 0) {
            return false;
        }
        return delegate.upgradeEncoding(encodedPassword.substring(delimIdx + 1));
    }
}

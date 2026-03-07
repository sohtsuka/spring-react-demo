package com.example.app.util;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import com.example.app.security.PepperPasswordEncoder;

import java.io.PrintStream;

/**
 * Gradle の hashPassword タスクから呼び出される CLI ユーティリティ。
 * 指定されたパスワードとペッパーを PepperPasswordEncoder でハッシュ化して標準出力に表示する。
 *
 * 使い方:
 *   ./gradlew hashPassword -Ppassword=Password1! -Ppepper=dev-pepper-change-in-production
 */
public class PasswordHasher {

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length < 1 || args[0].isBlank()) {
            err.println("使い方: ./gradlew hashPassword -Ppassword=<パスワード> [-Ppepper=<ペッパー>]");
            return 1;
        }

        String password = args[0];
        String pepper   = args.length > 1 ? args[1] : "";

        // SecurityConfig と同じパラメーター
        Argon2PasswordEncoder argon2 = new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
        PepperPasswordEncoder encoder = new PepperPasswordEncoder(argon2, pepper);
        out.println(encoder.encode(password));
        return 0;
    }
}

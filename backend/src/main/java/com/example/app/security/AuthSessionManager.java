package com.example.app.security;

import java.util.stream.IntStream;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.stereotype.Component;

/** 同一ユーザーの同時ログインでも確認・登録・保存を一つの操作として扱う。 */
@Component
public class AuthSessionManager {
    private final Object[] locks = IntStream.range(0, 64).mapToObj(ignored -> new Object()).toArray();
    private final SessionAuthenticationStrategy strategy;
    private final SecurityContextRepository contexts;
    private final CookieCsrfTokenRepository csrf;

    public AuthSessionManager(SessionAuthenticationStrategy strategy, SecurityContextRepository contexts,
            CookieCsrfTokenRepository csrf) {
        this.strategy = strategy;
        this.contexts = contexts;
        this.csrf = csrf;
    }

    public void login(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        synchronized (locks[Math.floorMod(Long.hashCode(principal.getAccountId()), locks.length)]) {
            strategy.onAuthentication(authentication, request, response);
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            contexts.saveContext(context, request, response);
            // CSRF フィルターの cookie 出力後に認証するため、新しい token をここで配布する。
            csrf.saveToken(csrf.generateToken(request), request, response);
        }
    }
}

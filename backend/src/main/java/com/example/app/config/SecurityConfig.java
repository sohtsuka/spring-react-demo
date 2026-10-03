package com.example.app.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionFixationProtectionStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.session.ConcurrentSessionFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.app.model.dto.ErrorResponse;
import com.example.app.repository.UserRepository;
import com.example.app.security.PepperPasswordEncoder;
import com.example.app.security.SessionValidationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    // Spring Boot 4 の初期化順序問題を避けるため ObjectMapper をインジェクションせず直接生成
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Value("${app.security.pepper}")
    private String pepper;

    public SecurityConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserRepository users,
            CookieCsrfTokenRepository csrfTokenRepository, SessionRegistry registry,
            SessionAuthenticationStrategy sessionStrategy, SecurityContextRepository contexts) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();

        http.csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository)
                // ログインは新規セッション生成のため CSRF 対象外
                .ignoringRequestMatchers("/api/v1/auth/login").csrfTokenRequestHandler(requestHandler))
                // レスポンスごとに CSRF クッキーを書き出すフィルター
                .addFilterAfter(new CsrfCookieFilter(), org.springframework.security.web.csrf.CsrfFilter.class)
                .addFilterBefore(new SessionValidationFilter(users), AuthorizationFilter.class)
                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/users/**").hasAnyRole("ADMIN", "MANAGER").anyRequest()
                        .authenticated())
                .securityContext(context -> context.securityContextRepository(contexts))
                .sessionManagement(session -> session.sessionAuthenticationStrategy(sessionStrategy))
                .addFilterAt(new ConcurrentSessionFilter(registry, event -> {
                    var response = event.getResponse();
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.getWriter().write(
                            objectMapper.writeValueAsString(ErrorResponse.of("UNAUTHORIZED", "別のログインによりセッションが失効しました")));
                }), ConcurrentSessionFilter.class)
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.getWriter()
                            .write(objectMapper.writeValueAsString(ErrorResponse.of("UNAUTHORIZED", "認証が必要です")));
                }).accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.getWriter()
                            .write(objectMapper.writeValueAsString(ErrorResponse.of("FORBIDDEN", "この操作を行う権限がありません")));
                })).headers(headers -> headers.contentTypeOptions(ct -> {
                }).frameOptions(fo -> fo.deny())
                        .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000).includeSubDomains(true))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        // この API は JSON しか返さないため、ブラウザーが何も読み込まない CSP にする。
                        // frame-ancestors は X-Frame-Options: DENY の現代版で、埋め込みも禁止する。
                        .contentSecurityPolicy(
                                csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'")));

        return http.build();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy(SessionRegistry registry) {
        var concurrent = new ConcurrentSessionControlAuthenticationStrategy(registry);
        concurrent.setMaximumSessions(1);
        var fixation = new SessionFixationProtectionStrategy();
        fixation.setMigrateSessionAttributes(false);
        return new CompositeSessionAuthenticationStrategy(
                List.of(concurrent, fixation, new RegisterSessionAuthenticationStrategy(registry)));
    }

    /** Spring Security 6/7 の遅延 CSRF トークンを毎レスポンスでクッキーに書き出す */
    private static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                // getToken() を呼ぶことで遅延評価を解決し、クッキーを書き出す
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // Spring Security 7: UserDetailsService をコンストラクタで渡す
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Argon2id: saltLength=16, hashLength=32, parallelism=1, memory=65536, iterations=3
        Argon2PasswordEncoder argon2 = new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
        return new PepperPasswordEncoder(argon2, pepper);
    }
}

package com.example.app.util;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.app.exception.ErrorCode;
import com.example.app.model.dto.ErrorResponse;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS_PER_MINUTE = 60;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * アプリの手前にある信頼できるリバースプロキシの段数。
     *
     * <p>
     * X-Forwarded-For はクライアントが自由に付けられるため、先頭要素を信用するとレート制限を
     * 迂回できてしまう。信頼できるプロキシが付けた要素だけを使うため、右から数えた位置を採用する。
     * 既定の 0 は「プロキシ無し」= X-Forwarded-For を一切信用しないことを意味する。
     */
    private final int trustedProxyCount;

    public RateLimitInterceptor(@Value("${app.security.trusted-proxy-count:0}") int trustedProxyCount) {
        this.trustedProxyCount = trustedProxyCount;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String ip = resolveClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, this::newBucket);

        if (bucket.tryConsume(1)) {
            return true;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(
                ErrorResponse.of(ErrorCode.RATE_LIMIT_EXCEEDED.getCode(), ErrorCode.RATE_LIMIT_EXCEEDED.getMessage())));
        return false;
    }

    private Bucket newBucket(String ip) {
        Bandwidth limit = Bandwidth.builder().capacity(MAX_REQUESTS_PER_MINUTE)
                .refillGreedy(MAX_REQUESTS_PER_MINUTE, Duration.ofMinutes(1)).build();
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * レート制限のキーにするクライアント IP。
     *
     * <p>
     * 信頼できるプロキシが N 段あるとき、X-Forwarded-For の右から N 番目が本来のクライアントになる
     * (各プロキシが自分の受信元を右に追記していくため)。攻撃者が前方に足した値はその左側に残るので
     * 無視される。段数が足りないリクエストは信用せず接続元アドレスを使う。
     */
    private String resolveClientIp(HttpServletRequest request) {
        if (trustedProxyCount <= 0) {
            return request.getRemoteAddr();
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor == null || xForwardedFor.isBlank()) {
            return request.getRemoteAddr();
        }
        String[] entries = xForwardedFor.split(",");
        int index = entries.length - trustedProxyCount;
        if (index < 0) {
            return request.getRemoteAddr();
        }
        String clientIp = entries[index].trim();
        return clientIp.isEmpty() ? request.getRemoteAddr() : clientIp;
    }
}

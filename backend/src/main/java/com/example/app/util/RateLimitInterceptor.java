package com.example.app.util;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.TimeMeter;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.app.exception.ErrorCode;
import com.example.app.model.dto.ErrorResponse;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS_PER_MINUTE = 60;

    /** 個別のバケットを持つクライアント IP の上限。超えた分は {@link #overflowBucket} を共有する。 */
    static final int MAX_TRACKED_CLIENTS = 10_000;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int maxTrackedClients;
    private final TimeMeter timeMeter;
    /**
     * 追跡上限を超えた新しい IP が共有するバケット。
     *
     * <p>
     * IP を次々に変える攻撃 (IPv6 の大きなアドレス空間など) で {@link #buckets} が際限なく増え、
     * メモリを使い切るのを防ぐ。上限に達している間の新しい IP はまとめて 1 つの枠で制限する。
     */
    private final Bucket overflowBucket;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    /**
     * アプリの手前にある信頼できるリバースプロキシの段数。
     *
     * <p>
     * X-Forwarded-For はクライアントが自由に付けられるため、先頭要素を信用するとレート制限を
     * 迂回できてしまう。信頼できるプロキシが付けた要素だけを使うため、右から数えた位置を採用する。
     * 既定の 0 は「プロキシ無し」= X-Forwarded-For を一切信用しないことを意味する。
     */
    private final int trustedProxyCount;

    @Autowired
    public RateLimitInterceptor(@Value("${app.security.trusted-proxy-count:0}") int trustedProxyCount) {
        this(trustedProxyCount, MAX_TRACKED_CLIENTS, TimeMeter.SYSTEM_MILLISECONDS);
    }

    RateLimitInterceptor(int trustedProxyCount, int maxTrackedClients, TimeMeter timeMeter) {
        Assert.isTrue(maxTrackedClients > 0, "maxTrackedClients must be positive");
        this.trustedProxyCount = trustedProxyCount;
        this.maxTrackedClients = maxTrackedClients;
        this.timeMeter = timeMeter;
        this.overflowBucket = newBucket();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String ip = resolveClientIp(request);
        Bucket bucket = bucketFor(ip);

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

    /**
     * 満杯まで回復したバケットを捨てる。
     *
     * <p>
     * 満杯のバケットは新しく作るバケットと同じ状態なので、捨てても制限の結果は変わらない。
     * 最後のリクエストから 1 分ほどで満杯に戻るため、アクセスの止んだ IP は次の掃除で消える。
     */
    @Scheduled(fixedDelay = 60_000)
    public void evictIdleBuckets() {
        buckets.values().removeIf(bucket -> bucket.getAvailableTokens() >= MAX_REQUESTS_PER_MINUTE);
    }

    int trackedClientCount() {
        return buckets.size();
    }

    private Bucket bucketFor(String ip) {
        Bucket existing = buckets.get(ip);
        if (existing != null) {
            return existing;
        }
        // 並行した追加でわずかに上限を超えることはあるが、増え続けることはない
        if (buckets.size() >= maxTrackedClients) {
            return overflowBucket;
        }
        return buckets.computeIfAbsent(ip, ignored -> newBucket());
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder().capacity(MAX_REQUESTS_PER_MINUTE)
                .refillGreedy(MAX_REQUESTS_PER_MINUTE, Duration.ofMinutes(1)).build();
        return Bucket.builder().withCustomTimePrecision(timeMeter).addLimit(limit).build();
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

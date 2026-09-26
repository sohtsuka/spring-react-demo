package com.example.app.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitInterceptorTest {

    /** 指定の IP を 60 回消費し、61 回目が 429 になるかを返す。 */
    private boolean isRateLimited(RateLimitInterceptor interceptor, MockHttpServletRequest request) throws Exception {
        for (int i = 0; i < 60; i++) {
            assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        }
        return !interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
    }

    @Test
    void preHandle_whenUnderLimit_returnsTrue() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(0);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void preHandle_whenOverLimit_returnsFalseAndWrites429() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(0);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.2");

        // バケットを枯渇させる (MAX_REQUESTS_PER_MINUTE = 60)
        for (int i = 0; i < 60; i++) {
            boolean ok = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
            assertThat(ok).isTrue();
        }

        MockHttpServletResponse limitedResponse = new MockHttpServletResponse();
        boolean result = interceptor.preHandle(request, limitedResponse, new Object());

        assertThat(result).isFalse();
        assertThat(limitedResponse.getStatus()).isEqualTo(429);
        assertThat(limitedResponse.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void preHandle_withoutTrustedProxy_ignoresXForwardedFor() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(0);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        request.addHeader("X-Forwarded-For", "203.0.113.1");

        // XFF を毎回変えても、接続元が同じならレート制限は迂回できない
        for (int i = 0; i < 60; i++) {
            MockHttpServletRequest spoofed = new MockHttpServletRequest();
            spoofed.setRemoteAddr("192.168.1.100");
            spoofed.addHeader("X-Forwarded-For", "10.1." + (i / 256) + "." + (i % 256));
            assertThat(interceptor.preHandle(spoofed, new MockHttpServletResponse(), new Object())).isTrue();
        }

        MockHttpServletRequest spoofed = new MockHttpServletRequest();
        spoofed.setRemoteAddr("192.168.1.100");
        spoofed.addHeader("X-Forwarded-For", "10.9.9.9");
        assertThat(interceptor.preHandle(spoofed, new MockHttpServletResponse(), new Object())).isFalse();
    }

    @Test
    void preHandle_withOneTrustedProxy_usesRightmostEntryAndIgnoresSpoofedPrefix() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(1);

        // 右から 1 番目 (プロキシが追記した本物) が同じなので、前方に足した値を変えても同じバケット
        for (int i = 0; i < 60; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("192.168.1.100");
            request.addHeader("X-Forwarded-For", "10.1." + (i / 256) + "." + (i % 256) + ", 203.0.113.9");
            assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        }

        MockHttpServletRequest limited = new MockHttpServletRequest();
        limited.setRemoteAddr("192.168.1.100");
        limited.addHeader("X-Forwarded-For", "10.9.9.9, 203.0.113.9");
        assertThat(interceptor.preHandle(limited, new MockHttpServletResponse(), new Object())).isFalse();

        // 右から 1 番目が別 IP なら別バケット
        MockHttpServletRequest other = new MockHttpServletRequest();
        other.setRemoteAddr("192.168.1.100");
        other.addHeader("X-Forwarded-For", "10.9.9.9, 203.0.113.10");
        assertThat(interceptor.preHandle(other, new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test
    void preHandle_withTwoTrustedProxies_usesSecondFromRight() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(2);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        // [spoofed, client, proxy1] → 右から 2 番目 = client
        request.addHeader("X-Forwarded-For", "10.9.9.9, 203.0.113.5, 192.168.10.1");

        assertThat(isRateLimited(interceptor, request)).isTrue();

        // 右から 2 番目が同じ client なら、前方と末尾が変わっても同じバケット
        MockHttpServletRequest same = new MockHttpServletRequest();
        same.setRemoteAddr("192.168.1.100");
        same.addHeader("X-Forwarded-For", "10.8.8.8, 203.0.113.5, 192.168.10.2");
        assertThat(interceptor.preHandle(same, new MockHttpServletResponse(), new Object())).isFalse();
    }

    @Test
    void preHandle_whenFewerHopsThanTrustedProxyCount_fallsBackToRemoteAddr() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(2);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.0.77");
        // 段数が足りない (1 要素しかない) → 信用せず接続元にフォールバック
        request.addHeader("X-Forwarded-For", "203.0.113.1");

        assertThat(isRateLimited(interceptor, request)).isTrue();

        // 同じ接続元なので、XFF が無いリクエストも同じバケットになる
        MockHttpServletRequest noHeader = new MockHttpServletRequest();
        noHeader.setRemoteAddr("192.168.0.77");
        assertThat(interceptor.preHandle(noHeader, new MockHttpServletResponse(), new Object())).isFalse();
    }

    @Test
    void preHandle_whenSelectedEntryIsBlank_fallsBackToRemoteAddr() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(2);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.0.88");
        // 右から 2 番目が空白 → 接続元にフォールバック
        request.addHeader("X-Forwarded-For", "   , 192.168.10.1");

        assertThat(isRateLimited(interceptor, request)).isTrue();

        MockHttpServletRequest noHeader = new MockHttpServletRequest();
        noHeader.setRemoteAddr("192.168.0.88");
        assertThat(interceptor.preHandle(noHeader, new MockHttpServletResponse(), new Object())).isFalse();
    }

    @Test
    void preHandle_withBlankXForwardedForHeader_usesRemoteAddr() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(1);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "   ");
        request.setRemoteAddr("192.168.0.51");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void preHandle_withoutXForwardedForHeader_usesRemoteAddr() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(1);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.0.50");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }
}

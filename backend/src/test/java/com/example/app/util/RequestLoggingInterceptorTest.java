package com.example.app.util;

import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestLoggingInterceptorTest {

    RequestLoggingInterceptor interceptor = new RequestLoggingInterceptor();

    @Test
    void afterCompletion_withStartTime_logsDuration() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.preHandle(request, response, new Object());
        // startTime 属性あり → 正常フロー（duration >= 0）
        interceptor.afterCompletion(request, response, new Object(), null);
    }

    @Test
    void afterCompletion_withoutStartTime_usesMinus1() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // preHandle を呼ばず startTime 属性なし → duration = -1
        interceptor.afterCompletion(request, response, new Object(), null);
    }
}

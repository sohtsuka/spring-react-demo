package com.example.app.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);

    private static final String ATTR_START_TIME = RequestLoggingInterceptor.class.getName() + ".startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(ATTR_START_TIME, System.currentTimeMillis());
        log.debug("Request: [{} {}] from {}",
                request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(ATTR_START_TIME);
        long duration = startTime != null ? System.currentTimeMillis() - startTime : -1;
        log.debug("Response: [{} {}] status={} duration={}ms",
                request.getMethod(), request.getRequestURI(), response.getStatus(), duration);
    }
}

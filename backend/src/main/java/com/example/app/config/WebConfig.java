package com.example.app.config;

import com.example.app.util.RateLimitInterceptor;
import com.example.app.util.RequestLoggingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RequestLoggingInterceptor requestLoggingInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    public WebConfig(RequestLoggingInterceptor requestLoggingInterceptor,
                     RateLimitInterceptor rateLimitInterceptor) {
        this.requestLoggingInterceptor = requestLoggingInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestLoggingInterceptor);
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/auth/login");
    }
}

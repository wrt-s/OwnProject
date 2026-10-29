package com.wrt.excel.system.config;

import com.wrt.excel.order.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Register auth interceptor + local upload static mapping
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcAuthConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Value("${love.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/css/**", "/js/**", "/images/**", "/uploads/**", "/favicon.ico",
                        "/error",
                        "/login",
                        "/api/wechat/callback"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String loc = java.nio.file.Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        if (!loc.endsWith("/")) {
            loc = loc + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(loc);
    }
}

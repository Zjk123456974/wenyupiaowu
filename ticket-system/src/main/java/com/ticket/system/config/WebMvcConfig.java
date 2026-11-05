package com.ticket.system.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
        registry.addResourceHandler("/api/uploads/**")
                .addResourceLocations("file:uploads/");
        registry.addResourceHandler("/api/js/**")
                .addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/api/css/**")
                .addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/api/images/**")
                .addResourceLocations("classpath:/static/images/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/",
                        "/page/**",
                        "/js/**",
                        "/css/**",
                        "/images/**",
                        "/uploads/**",
                        "/static/**",
                        // 注意：/upload 不再排除——上传需登录（头像/海报/广告），由 AuthInterceptor 校验 token 并填充 RequestContext
                        // Spring Boot 默认错误页：异常发生时 Tomcat 会向内 dispatch 到 /error，
                        // 若不排除，错误页自身会因未登录再抛一次 401，把真实错误掩盖成“未登录”
                        "/error",
                        "/user/register",
                        "/user/login",
                        "/activity/list",
                        "/activity/detail/**",
                        "/activity/hot",
                        "/activity/recommend",
                        "/activity/*/lineup",
                        "/category/list",
                        "/announcement/list",
                        "/announcement/detail/**",
                        "/banner/list",
                        "/admin/config/public",
                        "/ad/active",
                        "/ticket-price/activity/**",
                        "/seat/map",
                        "/tag/enabled",
                        "/tag/activity/**",
                        "/review/public/**",
                        "/review/statistics/**",
                        "/favicon.ico"
                );
    }
}

package com.jjxmas.ainovelstudio.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置，允许前端开发环境访问后端接口。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    /**
     * 注册全局 CORS 映射规则。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 匹配所有接口
                .allowedOrigins("*") // 允许所有前端域名（开发用，生产替换前端地址）
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") // 允许请求方式
                .allowCredentials(false) // * 时不能开cookie，如需true要写固定域名
                .maxAge(3600); // 预检请求有效期1小时
    }
}

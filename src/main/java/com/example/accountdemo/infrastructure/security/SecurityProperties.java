package com.example.accountdemo.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Bind {@code security.*} từ application.yml (public-paths...).
 *
 * <p><b>Vì sao cần class này:</b> thêm path permitAll trong yaml mà không sửa SecurityConfig.
 */
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private List<String> publicPaths = new ArrayList<>();
    private List<String> allowedOrigins = new ArrayList<>();

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}

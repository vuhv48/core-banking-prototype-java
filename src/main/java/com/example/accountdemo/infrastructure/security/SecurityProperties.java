package com.example.accountdemo.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Bind cấu hình security.* từ application.yml.
 *
 * public-paths: danh sách path không cần JWT (permitAll).
 * Thêm path mới trong yaml mà không cần sửa SecurityConfig.
 */
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private List<String> publicPaths = new ArrayList<>();

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }
}

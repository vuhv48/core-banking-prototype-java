package com.example.accountdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Điểm vào Spring Boot của ứng dụng bank/exchange demo.
 *
 * <p><b>Vì sao cần class này:</b> kích hoạt component scan và bootstrap toàn bộ context
 * (API, application, infrastructure) khi chạy {@code main}.
 */
@SpringBootApplication
public class AccountdemoApplication {

    /** Khởi động ứng dụng. */
    public static void main(String[] args) {
        SpringApplication.run(AccountdemoApplication.class, args);
    }
}

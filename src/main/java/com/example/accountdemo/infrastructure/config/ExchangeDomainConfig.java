package com.example.accountdemo.infrastructure.config;

import com.example.accountdemo.domain.exchange.OrderMatchingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wire Domain Service thuần Java vào Spring context (không gắn @Service vào domain/).
 */
@Configuration
public class ExchangeDomainConfig {

    @Bean
    public OrderMatchingService orderMatchingService() {
        return new OrderMatchingService();
    }
}

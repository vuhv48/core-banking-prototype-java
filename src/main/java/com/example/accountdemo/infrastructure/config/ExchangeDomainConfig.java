package com.example.accountdemo.infrastructure.config;

import com.example.accountdemo.domain.exchange.matching.OrderMatchingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wire Domain Service thuần Java vào Spring context.
 *
 * <p><b>Vì sao cần class này:</b> domain không gắn {@code @Service}; config này đăng ký bean
 * {@code OrderMatchingService} để application inject được.
 */
@Configuration
public class ExchangeDomainConfig {

    /** Bean matching thuần Java (không @Service trên domain). */
    @Bean
    public OrderMatchingService orderMatchingService() {
        return new OrderMatchingService();
    }
}

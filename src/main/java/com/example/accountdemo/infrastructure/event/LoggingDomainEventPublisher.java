package com.example.accountdemo.infrastructure.event;

import com.example.accountdemo.domain.exchange.event.DomainEventPublisher;
import com.example.accountdemo.domain.exchange.event.TradeExecutedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter DomainEventPublisher — chỉ log ra console.
 *
 * <p><b>Vì sao cần class này:</b> đóng port domain bằng implementation tạm; sau này Kafka/Outbox
 * thay bean mà không đổi application.
 */
@Slf4j
@Component
public class LoggingDomainEventPublisher implements DomainEventPublisher {

    /** Log sự kiện khớp lệnh (placeholder trước Kafka/Outbox). */
    @Override
    public void publish(TradeExecutedEvent event) {
        log.info("Trade executed: {}", event);
    }
}

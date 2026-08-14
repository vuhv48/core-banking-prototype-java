package com.example.accountdemo.infrastructure.config;

import com.example.accountdemo.domain.account.model.Account;
import com.example.accountdemo.domain.account.AccountRepository;
import com.example.accountdemo.domain.account.model.AccountStatus;
import com.example.accountdemo.domain.account.model.Balance;
import com.example.accountdemo.domain.exchange.orderbook.model.OrderBook;
import com.example.accountdemo.domain.exchange.orderbook.OrderBookRepository;
import com.example.accountdemo.domain.exchange.shared.TradingPair;
import com.example.accountdemo.infrastructure.persistence.account.AccountJpaRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Nạp dữ liệu mẫu qua repository port khi chạy profile {@code seed}.
 *
 * Chạy: mvn spring-boot:run -Dspring-boot.run.profiles=seed
 */
@Configuration
@Profile("seed")
public class SeedDataConfig {

    private static final TradingPair BTC_VND = new TradingPair("BTC", "VND");

    @Bean
    CommandLineRunner seedData(
            AccountJpaRepository accountJpaRepository,
            AccountRepository accountRepository,
            OrderBookRepository orderBookRepository
    ) {
        return args -> {
            if (accountJpaRepository.count() > 0) {
                return;
            }

            accountRepository.save(accountWith("ACC-001", AccountStatus.ACTIVE, 10_000_000, 5));
            accountRepository.save(accountWith("ACC-002", AccountStatus.ACTIVE, 5_000_000, 5));
            accountRepository.save(accountWith("ACC-003", AccountStatus.FROZEN, 2_000_000, 0));

            orderBookRepository.save(new OrderBook(BTC_VND));
        };
    }

    private static Account accountWith(String id, AccountStatus status, long vnd, long btc) {
        Map<String, Balance> holdings = new LinkedHashMap<>();
        holdings.put("VND", new Balance("VND", vnd, 0));
        if (btc > 0) {
            holdings.put("BTC", new Balance("BTC", btc, 0));
        }
        return new Account(id, status, holdings);
    }
}

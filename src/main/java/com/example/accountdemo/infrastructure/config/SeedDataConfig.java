package com.example.accountdemo.infrastructure.config;

import com.example.accountdemo.domain.account.Account;
import com.example.accountdemo.domain.account.AccountRepository;
import com.example.accountdemo.domain.account.AccountStatus;
import com.example.accountdemo.domain.account.Money;
import com.example.accountdemo.domain.exchange.Order;
import com.example.accountdemo.domain.exchange.OrderBook;
import com.example.accountdemo.domain.exchange.OrderBookRepository;
import com.example.accountdemo.domain.exchange.OrderSide;
import com.example.accountdemo.domain.exchange.OrderStatus;
import com.example.accountdemo.domain.exchange.OrderType;
import com.example.accountdemo.domain.exchange.Price;
import com.example.accountdemo.domain.exchange.Quantity;
import com.example.accountdemo.domain.exchange.TradingPair;
import com.example.accountdemo.infrastructure.persistence.account.AccountJpaRepository;
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

            accountRepository.save(new Account("ACC-001", new Money(10_000_000, "VND"), AccountStatus.ACTIVE));
            accountRepository.save(new Account("ACC-002", new Money(5_000_000, "VND"), AccountStatus.ACTIVE));
            accountRepository.save(new Account("ACC-003", new Money(2_000_000, "VND"), AccountStatus.FROZEN));

            OrderBook orderBook = new OrderBook(BTC_VND);
            orderBook.addOrder(new Order(
                    "ORD-BUY-001", "ACC-001", OrderSide.BUY, OrderType.LIMIT,
                    BTC_VND, new Quantity(100), new Price(60_000_000)
            ));
            orderBook.addOrder(new Order(
                    "ORD-BUY-002", "ACC-001", OrderSide.BUY, OrderType.LIMIT,
                    BTC_VND, new Quantity(50), new Price(59_500_000)
            ));
            orderBook.addOrder(Order.reconstitute(
                    "ORD-BUY-003", "ACC-002", OrderSide.BUY, OrderType.LIMIT,
                    BTC_VND, new Quantity(30), new Price(58_000_000),
                    new Quantity(10), OrderStatus.PARTIALLY_FILLED
            ));
            orderBook.addOrder(new Order(
                    "ORD-SELL-001", "ACC-002", OrderSide.SELL, OrderType.LIMIT,
                    BTC_VND, new Quantity(50), new Price(61_000_000)
            ));
            orderBook.addOrder(new Order(
                    "ORD-SELL-002", "ACC-002", OrderSide.SELL, OrderType.LIMIT,
                    BTC_VND, new Quantity(80), new Price(62_000_000)
            ));
            orderBook.addOrder(Order.reconstitute(
                    "ORD-SELL-003", "ACC-001", OrderSide.SELL, OrderType.LIMIT,
                    BTC_VND, new Quantity(20), new Price(63_500_000),
                    new Quantity(20), OrderStatus.FILLED
            ));

            orderBookRepository.save(orderBook);
        };
    }
}

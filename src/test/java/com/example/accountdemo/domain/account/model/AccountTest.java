package com.example.accountdemo.domain.account.model;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountTest {

    private Account accountWithVnd(String accountId, AccountStatus status, long vndAvailable) {
        Map<String, Balance> holdings = new LinkedHashMap<>();
        holdings.put("VND", new Balance("VND", vndAvailable, 0));
        return new Account(accountId, status, holdings);
    }

    @Test
    void withdraw_shouldReduceBalance() {
        Account account = accountWithVnd("ACC-001", AccountStatus.ACTIVE, 100_000);

        account.withdraw(new Money(30_000, "VND"));

        assertEquals(0, BigDecimal.valueOf(70_000).compareTo(account.getAvailable("VND").getAmount()));
    }

    @Test
    void withdraw_shouldThrowWhenInsufficientBalance() {
        Account account = accountWithVnd("ACC-001", AccountStatus.ACTIVE, 50_000);

        assertThrows(IllegalArgumentException.class,
                () -> account.withdraw(new Money(100_000, "VND")));
    }

    @Test
    void withdraw_shouldAllowWithdrawingExactBalance() {
        Account account = accountWithVnd("ACC-001", AccountStatus.ACTIVE, 100_000);

        account.withdraw(new Money(100_000, "VND"));

        assertEquals(0, BigDecimal.ZERO.compareTo(account.getAvailable("VND").getAmount()));
    }

    @Test
    void withdraw_shouldThrowWhenAccountIsFrozen() {
        Account account = accountWithVnd("ACC-001", AccountStatus.FROZEN, 100_000);

        assertThrows(IllegalStateException.class,
                () -> account.withdraw(new Money(10_000, "VND")));
    }

    @Test
    void deposit_shouldIncreaseBalance() {
        Account account = accountWithVnd("ACC-001", AccountStatus.ACTIVE, 100_000);

        account.deposit(new Money(50_000, "VND"));

        assertEquals(0, BigDecimal.valueOf(150_000).compareTo(account.getAvailable("VND").getAmount()));
    }

    @Test
    void deposit_shouldAllowWhenAccountIsFrozen() {
        Account account = accountWithVnd("ACC-001", AccountStatus.FROZEN, 100_000);

        account.deposit(new Money(50_000, "VND"));

        assertEquals(0, BigDecimal.valueOf(150_000).compareTo(account.getAvailable("VND").getAmount()));
    }

    @Test
    void deposit_shouldThrowWhenAmountIsZero() {
        Account account = accountWithVnd("ACC-001", AccountStatus.ACTIVE, 100_000);

        assertThrows(IllegalArgumentException.class,
                () -> account.deposit(new Money(0, "VND")));
    }

    @Test
    void constructor_shouldThrowWhenAccountIdIsBlank() {
        Map<String, Balance> holdings = Map.of("VND", new Balance("VND", 100, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Account("", AccountStatus.ACTIVE, holdings));
    }

    @Test
    void reserve_shouldMoveAvailableToLocked() {
        Account account = accountWithVnd("ACC-001", AccountStatus.ACTIVE, 10_000_000);

        account.reserve(new Money(1_000_000, "VND"));

        assertEquals(0, BigDecimal.valueOf(9_000_000).compareTo(account.getAvailable("VND").getAmount()));
        assertEquals(0, BigDecimal.valueOf(1_000_000).compareTo(account.getLocked("VND").getAmount()));
    }

    @Test
    void reserve_shouldFailWhenInsufficientAvailable() {
        Account account = accountWithVnd("ACC-001", AccountStatus.ACTIVE, 500_000);

        assertThrows(IllegalArgumentException.class,
                () -> account.reserve(new Money(1_000_000, "VND")));
    }

    @Test
    void release_shouldReturnLockedToAvailable() {
        Account account = accountWithVnd("ACC-001", AccountStatus.ACTIVE, 10_000_000);
        account.reserve(new Money(1_000_000, "VND"));

        account.release(new Money(1_000_000, "VND"));

        assertEquals(0, BigDecimal.valueOf(10_000_000).compareTo(account.getAvailable("VND").getAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(account.getLocked("VND").getAmount()));
    }

    @Test
    void consumeLocked_andCredit_shouldSettleTrade() {
        Map<String, Balance> buyerHoldings = new LinkedHashMap<>();
        buyerHoldings.put("VND", new Balance("VND", 9_000_000, 1_000_000));
        buyerHoldings.put("BTC", new Balance("BTC", 0, 0));
        Account buyer = new Account("ACC-001", AccountStatus.ACTIVE, buyerHoldings);

        Map<String, Balance> sellerHoldings = new LinkedHashMap<>();
        sellerHoldings.put("VND", new Balance("VND", 0, 0));
        sellerHoldings.put("BTC", new Balance("BTC", 0, 1));
        Account seller = new Account("ACC-002", AccountStatus.ACTIVE, sellerHoldings);

        buyer.consumeLocked(new Money(1_000_000, "VND"));
        buyer.credit(new Money(1, "BTC"));
        seller.consumeLocked(new Money(1, "BTC"));
        seller.credit(new Money(1_000_000, "VND"));

        assertEquals(0, BigDecimal.valueOf(9_000_000).compareTo(buyer.getAvailable("VND").getAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(buyer.getLocked("VND").getAmount()));
        assertEquals(0, BigDecimal.valueOf(1).compareTo(buyer.getAvailable("BTC").getAmount()));
        assertEquals(0, BigDecimal.valueOf(1_000_000).compareTo(seller.getAvailable("VND").getAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(seller.getAvailable("BTC").getAmount()));
    }
}

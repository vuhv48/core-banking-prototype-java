package com.example.accountdemo.domain.account;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountTest {

    private Account activeAccount(long balance) {
        return new Account("ACC-001", new Money(balance, "VND"), AccountStatus.ACTIVE);
    }

    @Test
    void withdraw_shouldReduceBalance() {
        Account account = activeAccount(100_000);

        account.withdraw(new Money(30_000, "VND"));

        assertEquals(70_000, account.getBalance().getAmount());
    }

    @Test
    void withdraw_shouldThrowWhenInsufficientBalance() {
        Account account = activeAccount(50_000);

        assertThrows(IllegalArgumentException.class,
                () -> account.withdraw(new Money(100_000, "VND")));
    }

    @Test
    void withdraw_shouldAllowWithdrawingExactBalance() {
        Account account = activeAccount(100_000);

        account.withdraw(new Money(100_000, "VND"));

        assertEquals(0, account.getBalance().getAmount());
    }

    @Test
    void withdraw_shouldThrowWhenAccountIsFrozen() {
        Account account = new Account("ACC-001", new Money(100_000, "VND"), AccountStatus.FROZEN);

        assertThrows(IllegalStateException.class,
                () -> account.withdraw(new Money(10_000, "VND")));
    }

    @Test
    void deposit_shouldIncreaseBalance() {
        Account account = activeAccount(100_000);

        account.deposit(new Money(50_000, "VND"));

        assertEquals(150_000, account.getBalance().getAmount());
    }

    @Test
    void deposit_shouldAllowWhenAccountIsFrozen() {
        Account account = new Account("ACC-001", new Money(100_000, "VND"), AccountStatus.FROZEN);

        account.deposit(new Money(50_000, "VND"));

        assertEquals(150_000, account.getBalance().getAmount());
    }

    @Test
    void deposit_shouldThrowWhenAmountIsZero() {
        Account account = activeAccount(100_000);

        assertThrows(IllegalArgumentException.class,
                () -> account.deposit(new Money(0, "VND")));
    }

    @Test
    void constructor_shouldThrowWhenAccountIdIsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new Account("", new Money(100, "VND"), AccountStatus.ACTIVE));
    }

    @Test
    void reserve_shouldMoveAvailableToLocked() {
        Account account = activeAccount(10_000_000);

        account.reserve(new Money(1_000_000, "VND"));

        assertEquals(9_000_000, account.getAvailable("VND").getAmount());
        assertEquals(1_000_000, account.getLocked("VND").getAmount());
    }

    @Test
    void reserve_shouldFailWhenInsufficientAvailable() {
        Account account = activeAccount(500_000);

        assertThrows(IllegalArgumentException.class,
                () -> account.reserve(new Money(1_000_000, "VND")));
    }

    @Test
    void release_shouldReturnLockedToAvailable() {
        Account account = activeAccount(10_000_000);
        account.reserve(new Money(1_000_000, "VND"));

        account.release(new Money(1_000_000, "VND"));

        assertEquals(10_000_000, account.getAvailable("VND").getAmount());
        assertEquals(0, account.getLocked("VND").getAmount());
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

        assertEquals(9_000_000, buyer.getAvailable("VND").getAmount());
        assertEquals(0, buyer.getLocked("VND").getAmount());
        assertEquals(1, buyer.getAvailable("BTC").getAmount());
        assertEquals(1_000_000, seller.getAvailable("VND").getAmount());
        assertEquals(0, seller.getAvailable("BTC").getAmount());
    }
}

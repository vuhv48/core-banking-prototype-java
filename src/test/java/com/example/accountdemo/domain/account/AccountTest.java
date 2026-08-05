package com.example.accountdemo.domain.account;

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
}

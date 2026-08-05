package com.example.accountdemo.domain.account;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {

    @Test
    void add_shouldSumSameCurrency() {
        Money a = new Money(100_000, "VND");
        Money b = new Money(50_000, "VND");

        Money result = a.add(b);

        assertEquals(150_000, result.getAmount());
        assertEquals("VND", result.getCurrency());
    }

    @Test
    void subtract_shouldReturnDifference() {
        Money a = new Money(100_000, "VND");
        Money b = new Money(30_000, "VND");

        Money result = a.subtract(b);

        assertEquals(70_000, result.getAmount());
    }

    @Test
    void add_shouldThrowWhenDifferentCurrency() {
        Money vnd = new Money(100, "VND");
        Money usd = new Money(50, "USD");

        assertThrows(IllegalArgumentException.class, () -> vnd.add(usd));
    }

    @Test
    void isNegative_shouldReturnTrueWhenAmountLessThanZero() {
        Money money = new Money(-1, "VND");

        assertTrue(money.isNegative());
    }

    @Test
    void isGreaterThan_shouldReturnTrueWhenAmountIsLarger() {
        Money larger = new Money(100_000, "VND");
        Money smaller = new Money(50_000, "VND");

        assertTrue(larger.isGreaterThan(smaller));
    }

    @Test
    void constructor_shouldThrowWhenCurrencyIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Money(100, ""));
        assertThrows(IllegalArgumentException.class, () -> new Money(100, null));
    }

    @Test
    void isNegative_shouldReturnFalseForZero() {
        Money money = new Money(0, "VND");

        assertFalse(money.isNegative());
    }
}

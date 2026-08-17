package com.example.accountdemo.api.dto;

/** Một dòng số dư trong response xem ví. */
public record AccountBalanceItem(String currency, long available, long locked) {
}

package com.example.accountdemo.api.dto;

import java.math.BigDecimal;

/** Một dòng số dư trong response xem ví. */
public record AccountBalanceItem(String currency, BigDecimal available, BigDecimal locked) {
}

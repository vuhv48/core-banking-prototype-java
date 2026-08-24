package com.example.accountdemo.api.dto;

import java.math.BigDecimal;

/** Một dòng số dư trong body tạo account (locked = 0 ở Application). */
public record HoldingRequestItem(String currency, BigDecimal available) {
}

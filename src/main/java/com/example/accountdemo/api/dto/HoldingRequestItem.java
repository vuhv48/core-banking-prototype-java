package com.example.accountdemo.api.dto;

/** Một dòng số dư trong body tạo account (locked = 0 ở Application). */
public record HoldingRequestItem(String currency, long available) {
}

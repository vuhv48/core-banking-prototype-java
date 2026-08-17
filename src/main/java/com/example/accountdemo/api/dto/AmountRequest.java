package com.example.accountdemo.api.dto;

/** Body nạp / rút tiền. */
public record AmountRequest(long amount, String currency) {
}

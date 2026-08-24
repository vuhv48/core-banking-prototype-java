package com.example.accountdemo.api.dto;

import java.math.BigDecimal;

/** Body nạp / rút tiền. */
public record AmountRequest(BigDecimal amount, String currency) {
}

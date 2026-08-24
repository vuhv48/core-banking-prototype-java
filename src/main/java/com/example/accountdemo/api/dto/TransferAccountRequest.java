package com.example.accountdemo.api.dto;

import java.math.BigDecimal;

public record TransferAccountRequest(
        String fromAccountId,
        String toAccountId,
        BigDecimal amount,
        String currency
) {
}

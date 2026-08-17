package com.example.accountdemo.api.dto;

public record TransferAccountRequest(
        String fromAccountId,
        String toAccountId,
        long amount,
        String currency
) {
}

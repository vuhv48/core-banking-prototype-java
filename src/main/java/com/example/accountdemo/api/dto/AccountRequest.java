package com.example.accountdemo.api.dto;

import java.util.List;

/** Body tạo account — chỉ DTO, không import domain. */
public record AccountRequest(
        String accountId,
        String status,
        List<HoldingRequestItem> holdings
) {
}

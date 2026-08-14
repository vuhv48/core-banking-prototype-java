package com.example.accountdemo.api.dto;

import com.example.accountdemo.domain.account.model.Account;
import com.example.accountdemo.domain.account.model.Balance;
import java.util.List;

/**
 * Response xem ví: accountId, status, danh sách số dư theo currency.
 *
 * <p><b>Vì sao cần class này:</b> không lộ aggregate domain ra JSON; chỉ trả available/locked cần thiết.
 */
public record AccountResponse(
        String accountId,
        String status,
        List<BalanceItem> balances
) {
    /** Một dòng số dư theo currency. */
    public record BalanceItem(String currency, long available, long locked) {
    }

    /** Map domain Account → DTO. */
    public static AccountResponse from(Account account) {
        List<BalanceItem> items = account.getHoldings().values().stream()
                .map(AccountResponse::toItem)
                .toList();
        return new AccountResponse(account.getAccountId(), account.getStatus().name(), items);
    }

    private static BalanceItem toItem(Balance balance) {
        return new BalanceItem(balance.getCurrency(), balance.getAvailable(), balance.getLocked());
    }
}

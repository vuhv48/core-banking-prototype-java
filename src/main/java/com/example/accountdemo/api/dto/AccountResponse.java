package com.example.accountdemo.api.dto;

import com.example.accountdemo.domain.account.model.Account;
import com.example.accountdemo.domain.account.model.Balance;
import java.util.List;

/** Response xem ví: accountId, status, danh sách số dư theo currency. */
public record AccountResponse(
        String accountId,
        String status,
        List<AccountBalanceItem> balances
) {
    public static AccountResponse from(Account account) {
        List<AccountBalanceItem> items = account.getHoldings().values().stream()
                .map(AccountResponse::toItem)
                .toList();
        return new AccountResponse(account.getAccountId(), account.getStatus().name(), items);
    }

    private static AccountBalanceItem toItem(Balance balance) {
        return new AccountBalanceItem(balance.getCurrency(), balance.getAvailable(), balance.getLocked());
    }
}

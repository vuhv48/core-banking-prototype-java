package com.example.accountdemo.api.dto;

import com.example.accountdemo.domain.account.Account;
import com.example.accountdemo.domain.account.Balance;
import java.util.List;

public record AccountResponse(
        String accountId,
        String status,
        List<BalanceItem> balances
) {
    public record BalanceItem(String currency, long available, long locked) {
    }

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

package com.example.accountdemo.application;

import com.example.accountdemo.domain.account.AccountRepository;
import com.example.accountdemo.domain.account.model.Account;
import com.example.accountdemo.domain.account.model.AccountStatus;
import com.example.accountdemo.domain.account.model.Balance;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateAccountApplicationService {

    private final AccountRepository accountRepository;

    /**
     * @param initialAvailable currency → available ban đầu (locked = 0). null/empty = ví trống.
     */
    public void createAccount(String accountId, String status, Map<String, Long> initialAvailable) {
        if (accountRepository.findById(accountId) != null) {
            throw new IllegalArgumentException("accountId đã tồn tại: " + accountId);
        }

        accountRepository.save(new Account(
                accountId,
                AccountStatus.valueOf(status),
                toHoldings(initialAvailable)
        ));
    }

    private static Map<String, Balance> toHoldings(Map<String, Long> initialAvailable) {
        Map<String, Balance> map = new LinkedHashMap<>();
        for (Map.Entry<String, Long> e : nullToEmpty(initialAvailable).entrySet()) {
            map.put(e.getKey(), new Balance(e.getKey(), e.getValue(), 0));
        }
        return map;
    }

    private static Map<String, Long> nullToEmpty(Map<String, Long> map) {
        return map == null ? Collections.emptyMap() : map;
    }
}

package com.example.accountdemo.application;

import com.example.accountdemo.api.common.DomainException;
import com.example.accountdemo.api.common.ErrorStatus;
import com.example.accountdemo.domain.account.model.Account;
import com.example.accountdemo.domain.account.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Application Service — use case xem số dư / thông tin tài khoản.
 * <p><b>Vì sao cần class này:</b> điều phối ownership + đọc Account, không để API
 * gọi thẳng repository và bỏ qua kiểm tra quyền.
 */
@Service
@RequiredArgsConstructor
public class GetAccountApplicationService {

    private final AccountRepository accountRepository;
    private final OwnershipChecker ownershipGuard;

    /**
     * Trả về Account sau khi xác nhận user được xem accountId đó.
     */
    @Transactional(readOnly = true)
    public Account get(String username, String accountId) {
        ownershipGuard.requireAccountAccess(username, accountId);
        Account account = accountRepository.findById(accountId);
        if (account == null) {
            throw new DomainException(ErrorStatus.ACCOUNT_NOT_FOUND);
        }
        return account;
    }
}

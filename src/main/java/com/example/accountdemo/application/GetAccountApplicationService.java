package com.example.accountdemo.application;

import com.example.accountdemo.api.common.DomainException;
import com.example.accountdemo.api.common.ErrorStatus;
import com.example.accountdemo.domain.account.AccountPage;
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

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

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

    /**
     * Danh sách account có phân trang — chỉ admin.
     * @param page trang 0-based (null → 0)
     * @param size kích thước trang (null → 20, max 100)
     */
    @Transactional(readOnly = true)
    public AccountPage listAll(String username, Integer page, Integer size) {
        ownershipGuard.requireAdmin(username);
        int p = page == null || page < 0 ? DEFAULT_PAGE : page;
        int s = size == null || size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return accountRepository.findPage(p, s);
    }
}

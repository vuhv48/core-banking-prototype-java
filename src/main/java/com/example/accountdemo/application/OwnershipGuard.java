package com.example.accountdemo.application;

import com.example.accountdemo.api.common.DomainException;
import com.example.accountdemo.api.common.ErrorStatus;
import com.example.accountdemo.domain.exchange.order.model.Order;
import com.example.accountdemo.infrastructure.persistence.security.UserJpaEntity;
import com.example.accountdemo.infrastructure.persistence.security.UserJpaRepository;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

/**
 * Triển khai OwnershipChecker: user gắn account chỉ được thao tác account/order đó;
 * user không gắn account (admin) được thao tác hộ.
 * <p><b>Vì sao cần class này:</b> ngăn trader A đụng tài khoản / lệnh của trader B,
 * đồng thời cho phép admin hỗ trợ khi không bind account.
 */
@Component
@RequiredArgsConstructor
public class OwnershipGuard implements OwnershipChecker {

    private final UserJpaRepository userJpaRepository;

    /**
     * So khớp accountId với account gắn user; lệch thì ACCOUNT_NOT_OWNED.
     */
    @Override
    public void requireAccountAccess(String username, String accountId) {
        String linked = linkedAccountId(username);
        if (linked != null && !linked.equals(accountId)) {
            throw new DomainException(ErrorStatus.ACCOUNT_NOT_OWNED);
        }
    }

    /**
     * Chỉ ROLE_ADMIN được gọi các use case toàn hệ thống (list all accounts…).
     */
    @Override
    public void requireAdmin(String username) {
        if (username == null || username.isBlank()) {
            throw new DomainException(ErrorStatus.UNAUTHORIZED);
        }
        if (!userJpaRepository.existsByUsername(username)) {
            throw new DomainException(ErrorStatus.USER_NOT_FOUND);
        }
        boolean isAdmin = userJpaRepository.findRoleNamesByUsername(username).stream()
                .anyMatch("ROLE_ADMIN"::equals);
        if (!isAdmin) {
            throw new DomainException(ErrorStatus.FORBIDDEN, "Chỉ admin được xem danh sách account");
        }
    }

    /**
     * Ủy quyền kiểm tra ownership qua accountId của lệnh.
     */
    @Override
    public void requireOrderAccess(String username, Order order) {
        requireAccountAccess(username, order.getAccountId());
    }

    private String linkedAccountId(String username) {
        if (username == null || username.isBlank()) {
            throw new DomainException(ErrorStatus.UNAUTHORIZED);
        }
        UserJpaEntity user = userJpaRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException(ErrorStatus.USER_NOT_FOUND));
        return user.getAccountId();
    }
}

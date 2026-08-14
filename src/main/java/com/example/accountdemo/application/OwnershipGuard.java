package com.example.accountdemo.application;

import com.example.accountdemo.api.common.DomainException;
import com.example.accountdemo.api.common.ErrorStatus;
import com.example.accountdemo.domain.exchange.order.model.Order;
import com.example.accountdemo.infrastructure.persistence.security.UserJpaEntity;
import com.example.accountdemo.infrastructure.persistence.security.UserJpaRepository;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

/**
 * Ownership: user có users.account_id thì chỉ được thao tác account/order đó.
 * User không gắn account (admin) → được thao tác hộ.
 */
@Component
@RequiredArgsConstructor
public class OwnershipGuard implements OwnershipChecker {

    private final UserJpaRepository userJpaRepository;

    @Override
    public void requireAccountAccess(String username, String accountId) {
        String linked = linkedAccountId(username);
        if (linked != null && !linked.equals(accountId)) {
            throw new DomainException(ErrorStatus.ACCOUNT_NOT_OWNED);
        }
    }

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

package com.example.accountdemo.application;

import com.example.accountdemo.api.common.DomainException;
import com.example.accountdemo.api.common.ErrorStatus;
import com.example.accountdemo.domain.account.AccountRepository;
import com.example.accountdemo.domain.account.model.Account;
import com.example.accountdemo.domain.account.model.AccountStatus;
import com.example.accountdemo.infrastructure.persistence.security.RoleJpaEntity;
import com.example.accountdemo.infrastructure.persistence.security.RoleJpaRepository;
import com.example.accountdemo.infrastructure.persistence.security.UserJpaEntity;
import com.example.accountdemo.infrastructure.persistence.security.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;

/**
 * Admin onboard user: tạo identity (ROLE_USER) + gắn ví có sẵn hoặc tạo ví mới.
 */
@Service
@RequiredArgsConstructor
public class AdminCreateUserApplicationService {

    private static final String DEFAULT_ROLE = "ROLE_USER";
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserJpaRepository userJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final OwnershipChecker ownershipChecker;

    /**
     * @param accountId null/blank = tạo account mới; ngược lại gắn account đã có
     */
    @Transactional
    public Result createUser(
            String adminUsername,
            String username,
            String password,
            String email,
            String accountId
    ) {
        ownershipChecker.requireAdmin(adminUsername);
        validate(username, password, email);

        if (userJpaRepository.existsByUsername(username.trim())) {
            throw new DomainException(ErrorStatus.USERNAME_ALREADY_EXISTS);
        }
        if (email != null && !email.isBlank() && userJpaRepository.existsByEmail(email.trim())) {
            throw new DomainException(ErrorStatus.EMAIL_ALREADY_EXISTS);
        }

        RoleJpaEntity role = roleJpaRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new DomainException(
                        ErrorStatus.INTERNAL_ERROR,
                        "Thiếu role mặc định: " + DEFAULT_ROLE
                ));

        String linkedAccountId = resolveAccountId(accountId);

        LocalDateTime now = LocalDateTime.now();
        UserJpaEntity user = UserJpaEntity.builder()
                .username(username.trim())
                .passwordHash(passwordEncoder.encode(password))
                .email(blankToNull(email))
                .accountId(linkedAccountId)
                .enabled(true)
                .roles(new LinkedHashSet<>())
                .directPermissions(new LinkedHashSet<>())
                .build();
        user.setDeleted(false);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.getRoles().add(role);
        userJpaRepository.save(user);

        return new Result(user.getUsername(), linkedAccountId);
    }

    private String resolveAccountId(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            String generated = "ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            accountRepository.save(new Account(generated, AccountStatus.ACTIVE, Map.of()));
            return generated;
        }

        String id = accountId.trim();
        if (accountRepository.findById(id) == null) {
            throw new DomainException(ErrorStatus.ACCOUNT_NOT_FOUND, "Không tìm thấy account: " + id);
        }
        if (userJpaRepository.existsByAccountId(id)) {
            throw new DomainException(ErrorStatus.ACCOUNT_ALREADY_LINKED);
        }
        return id;
    }

    private void validate(String username, String password, String email) {
        if (username == null || username.isBlank()) {
            throw new DomainException(ErrorStatus.VALIDATION_ERROR, "Username không được rỗng");
        }
        if (username.trim().length() < 3 || username.trim().length() > 100) {
            throw new DomainException(ErrorStatus.VALIDATION_ERROR, "Username phải từ 3–100 ký tự");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new DomainException(
                    ErrorStatus.VALIDATION_ERROR,
                    "Password phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự"
            );
        }
        if (email != null && !email.isBlank() && !email.contains("@")) {
            throw new DomainException(ErrorStatus.VALIDATION_ERROR, "Email không hợp lệ");
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record Result(String username, String accountId) {
    }
}

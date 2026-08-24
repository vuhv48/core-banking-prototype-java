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
 * Đăng ký user mới: tạo identity (ROLE_USER) + account trading rỗng gắn account_id.
 */
@Service
@RequiredArgsConstructor
public class RegisterApplicationService {

    private static final String DEFAULT_ROLE = "ROLE_USER";
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserJpaRepository userJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegisterResult register(String username, String password, String email) {
        validate(username, password, email);

        if (userJpaRepository.existsByUsername(username)) {
            throw new DomainException(ErrorStatus.USERNAME_ALREADY_EXISTS);
        }
        if (email != null && !email.isBlank() && userJpaRepository.existsByEmail(email)) {
            throw new DomainException(ErrorStatus.EMAIL_ALREADY_EXISTS);
        }

        RoleJpaEntity role = roleJpaRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new DomainException(
                        ErrorStatus.INTERNAL_ERROR,
                        "Thiếu role mặc định: " + DEFAULT_ROLE
                ));

        String accountId = generateAccountId();
        accountRepository.save(new Account(accountId, AccountStatus.ACTIVE, Map.of()));

        LocalDateTime now = LocalDateTime.now();
        UserJpaEntity user = UserJpaEntity.builder()
                .username(username.trim())
                .passwordHash(passwordEncoder.encode(password))
                .email(blankToNull(email))
                .accountId(accountId)
                .enabled(true)
                .roles(new LinkedHashSet<>())
                .directPermissions(new LinkedHashSet<>())
                .build();
        user.setDeleted(false);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.getRoles().add(role);

        userJpaRepository.save(user);

        return new RegisterResult(user.getUsername(), accountId);
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

    private static String generateAccountId() {
        return "ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record RegisterResult(String username, String accountId) {
    }
}

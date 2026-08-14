package com.example.accountdemo.domain.account.model;

/**
 * Enum trạng thái {@link Account} (không phải Value Object — tập giá trị cố định).
 *
 * <p>{@code ACTIVE}: nạp/rút/reserve bình thường.
 * <p>{@code FROZEN}: không withdraw/reserve; vẫn được deposit.
 */
public enum AccountStatus {
    ACTIVE,
    FROZEN
}

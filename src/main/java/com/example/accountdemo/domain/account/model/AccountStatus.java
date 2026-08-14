package com.example.accountdemo.domain.account.model;

/**
 * Enum trạng thái {@link Account} (không phải Value Object — tập giá trị cố định).
 *
 * <p><b>Vì sao cần:</b> phân biệt ví đang giao dịch bình thường với ví bị khóa,
 * để {@code withdraw}/{@code reserve} từ chối khi FROZEN mà vẫn cho {@code deposit}.
 */
public enum AccountStatus {
    /** Nạp/rút/reserve bình thường. */
    ACTIVE,
    /** Không withdraw/reserve; vẫn được deposit. */
    FROZEN
}

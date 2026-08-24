package com.example.accountdemo.api.common;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Mã lỗi API thống nhất (HTTP status + code + message mặc định).
 *
 * <p><b>Vì sao cần class này:</b> một nơi định nghĩa contract lỗi; DomainException + RestExceptionHandler
 * và filter security đều dùng chung để client nhận code ổn định.
 */
public enum ErrorStatus {

    // —— Xác thực / phân quyền ——
    AUTH_FAILED(401, "AUTH_FAILED", "Đăng nhập thất bại"),
    UNAUTHORIZED(401, "UNAUTHORIZED", "Chưa xác thực"),
    REFRESH_TOKEN_INVALID(401, "REFRESH_TOKEN_INVALID", "Refresh token không hợp lệ hoặc đã hết hạn"),
    FORBIDDEN(403, "FORBIDDEN", "Không đủ quyền"),
    ACCOUNT_DISABLED(403, "ACCOUNT_DISABLED", "Tài khoản đã bị khóa"),

    // —— Dữ liệu đầu vào ——
    VALIDATION_ERROR(400, "VALIDATION_ERROR", "Dữ liệu không hợp lệ"),
    INVALID_ARGUMENT(400, "INVALID_ARGUMENT", "Tham số không hợp lệ"),

    // —— Nghiệp vụ bank / exchange ——
    USERNAME_ALREADY_EXISTS(409, "USERNAME_ALREADY_EXISTS", "Username đã tồn tại"),
    EMAIL_ALREADY_EXISTS(409, "EMAIL_ALREADY_EXISTS", "Email đã tồn tại"),
    ACCOUNT_ALREADY_LINKED(409, "ACCOUNT_ALREADY_LINKED", "Account đã được gắn với user khác"),
    USER_NOT_FOUND(404, "USER_NOT_FOUND", "Không tìm thấy người dùng"),
    ACCOUNT_NOT_FOUND(404, "ACCOUNT_NOT_FOUND", "Không tìm thấy tài khoản"),
    ORDER_NOT_FOUND(404, "ORDER_NOT_FOUND", "Không tìm thấy lệnh"),
    ORDER_BOOK_NOT_OPEN(400, "ORDER_BOOK_NOT_OPEN", "Cặp giao dịch chưa được mở"),
    ORDER_INVALID(400, "ORDER_INVALID", "Lệnh không hợp lệ"),
    INSUFFICIENT_BALANCE(400, "INSUFFICIENT_BALANCE", "Số dư không đủ"),
    ACCOUNT_FROZEN(403, "ACCOUNT_FROZEN", "Tài khoản đang bị đóng băng"),
    ACCOUNT_NOT_OWNED(403, "ACCOUNT_NOT_OWNED", "Không được thao tác tài khoản này"),
    ORDER_NOT_OWNED(403, "ORDER_NOT_OWNED", "Không được thao tác lệnh này"),
    ORDER_NOT_CANCELLABLE(409, "ORDER_NOT_CANCELLABLE", "Lệnh không thể hủy"),
    MARKET_BUY_NOT_SUPPORTED(400, "MARKET_BUY_NOT_SUPPORTED", "Chưa hỗ trợ BUY MARKET (không xác định số VND cần giữ)"),
    DATA_INTEGRITY(409, "DATA_INTEGRITY", "Dữ liệu xung đột ràng buộc"),
    ILLEGAL_STATE(409, "ILLEGAL_STATE", "Trạng thái không hợp lệ"),

    // —— Hệ thống ——
    DATA_ACCESS_ERROR(500, "DATA_ACCESS_ERROR", "Không thực hiện được thao tác dữ liệu"),
    INTERNAL_ERROR(500, "INTERNAL_ERROR", "Lỗi hệ thống không mong đợi");

    private static final Map<String, ErrorStatus> BY_CODE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(ErrorStatus::code, Function.identity()));

    private final int httpStatus;
    private final String code;
    private final String defaultMessage;

    ErrorStatus(int httpStatus, String code, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    /** HTTP status tương ứng mã lỗi. */
    public int httpStatus() {
        return httpStatus;
    }

    /** Mã lỗi string gửi về client. */
    public String code() {
        return code;
    }

    /** Message mặc định khi caller không truyền message riêng. */
    public String defaultMessage() {
        return defaultMessage;
    }

    /** Tra ErrorStatus theo code (dùng khi map DomainException). */
    public static Optional<ErrorStatus> resolve(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_CODE.get(code));
    }
}

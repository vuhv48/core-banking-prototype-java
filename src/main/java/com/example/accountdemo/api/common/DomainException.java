package com.example.accountdemo.api.common;

/**
 * Exception nghiệp vụ mang mã {@link ErrorStatus}.
 * RestExceptionHandler map thành JSON {@link ApiResponse}.
 */
public class DomainException extends RuntimeException {

    private final String code;

    public DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public DomainException(ErrorStatus status) {
        super(status.defaultMessage());
        this.code = status.code();
    }

    public DomainException(ErrorStatus status, String message) {
        super(message);
        this.code = status.code();
    }

    public String getCode() {
        return code;
    }
}

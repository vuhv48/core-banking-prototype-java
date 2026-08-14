package com.example.accountdemo.api.error;

import com.example.accountdemo.api.common.ApiError;
import com.example.accountdemo.api.common.ApiResponse;
import com.example.accountdemo.api.common.DomainException;
import com.example.accountdemo.api.common.ErrorStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Chuẩn hóa mọi lỗi API thành {@link ApiResponse} + {@link ApiError}.
 *
 * <p><b>Vì sao cần class này:</b> một nơi map exception (security, domain, validation, DB) → JSON —
 * controller không try/catch lặp lại.
 */
@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    /** Sai username/password. */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> badCredentials(
            BadCredentialsException ex,
            HttpServletRequest req
    ) {
        log.warn("[api] Bad credentials uri={}", req.getRequestURI());
        return respond(ErrorStatus.AUTH_FAILED, ErrorStatus.AUTH_FAILED.defaultMessage(), req);
    }

    /** Tài khoản bị khóa. */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> disabled(DisabledException ex, HttpServletRequest req) {
        log.warn("[api] Account disabled uri={}", req.getRequestURI());
        return respond(ErrorStatus.ACCOUNT_DISABLED, ErrorStatus.ACCOUNT_DISABLED.defaultMessage(), req);
    }

    /** Chưa xác thực / lỗi authentication khác. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> authentication(
            AuthenticationException ex,
            HttpServletRequest req
    ) {
        log.warn("[api] Unauthorized uri={} type={}", req.getRequestURI(), ex.getClass().getSimpleName());
        return respond(ErrorStatus.UNAUTHORIZED, ErrorStatus.UNAUTHORIZED.defaultMessage(), req);
    }

    /** Không đủ quyền. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> accessDenied(
            AccessDeniedException ex,
            HttpServletRequest req
    ) {
        log.warn("[api] Forbidden uri={}", req.getRequestURI());
        return respond(ErrorStatus.FORBIDDEN, ErrorStatus.FORBIDDEN.defaultMessage(), req);
    }

    /** Lỗi nghiệp vụ có mã ErrorStatus. */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> domain(DomainException ex, HttpServletRequest req) {
        log.warn("[api] Domain uri={} code={} msg={}", req.getRequestURI(), ex.getCode(), ex.getMessage());
        return ErrorStatus.resolve(ex.getCode())
                .map(status -> respond(status, ex.getMessage(), req))
                .orElseGet(() -> respond(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), req));
    }

    /** Body JSON không parse được. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> httpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest req
    ) {
        log.warn("[api] Invalid request body uri={}", req.getRequestURI());
        return respond(ErrorStatus.VALIDATION_ERROR, "Body JSON không hợp lệ", req);
    }

    /** Tham số / invariant input không hợp lệ. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> illegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest req
    ) {
        log.warn("[api] Illegal argument uri={} msg={}", req.getRequestURI(), ex.getMessage());
        String message = ex.getMessage();
        if (message != null && message.contains("Cặp giao dịch chưa được mở")) {
            return respond(ErrorStatus.ORDER_BOOK_NOT_OPEN, message, req);
        }
        return respond(ErrorStatus.INVALID_ARGUMENT, message, req);
    }

    /** Trạng thái đối tượng không cho phép thao tác. */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> illegalState(
            IllegalStateException ex,
            HttpServletRequest req
    ) {
        log.warn("[api] Illegal state uri={} msg={}", req.getRequestURI(), ex.getMessage());
        return respond(ErrorStatus.ILLEGAL_STATE, ex.getMessage(), req);
    }

    /** Vi phạm ràng buộc DB (unique, FK...). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> dataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest req
    ) {
        log.warn("[api] Data integrity uri={}", req.getRequestURI());
        return respond(ErrorStatus.DATA_INTEGRITY, ErrorStatus.DATA_INTEGRITY.defaultMessage(), req);
    }

    /** Lỗi truy cập dữ liệu chung. */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> dataAccess(
            DataAccessException ex,
            HttpServletRequest req
    ) {
        log.error("[api] Data access uri={}", req.getRequestURI(), ex);
        return respond(ErrorStatus.DATA_ACCESS_ERROR, ErrorStatus.DATA_ACCESS_ERROR.defaultMessage(), req);
    }

    /** Fallback mọi exception chưa map. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> fallback(Exception ex, HttpServletRequest req) {
        log.error("[api] Unhandled exception uri={}", req.getRequestURI(), ex);
        return respond(ErrorStatus.INTERNAL_ERROR, ErrorStatus.INTERNAL_ERROR.defaultMessage(), req);
    }

    private static ResponseEntity<ApiResponse<Void>> respond(
            ErrorStatus status,
            String message,
            HttpServletRequest req
    ) {
        var error = ApiError.of(status, message, req.getRequestURI());
        return ResponseEntity.status(status.httpStatus()).body(ApiResponse.fail(error));
    }

    private static ResponseEntity<ApiResponse<Void>> respond(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest req
    ) {
        var error = ApiError.of(status.value(), code, message, req.getRequestURI());
        return ResponseEntity.status(status).body(ApiResponse.fail(error));
    }
}

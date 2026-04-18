package com.smartmes.backend.core.exception;

import com.smartmes.backend.core.common.ApiResponse;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String MSG_UNAUTHORIZED = "Unauthorized";
    private static final String MSG_FORBIDDEN = "Forbidden";
    private static final String MSG_NOT_FOUND = "Resource not found";
    private static final String MSG_INTERNAL_ERROR = "Internal server error. Please contact administrator.";

    // JWT Token hết hạn -> Return 401
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleExpiredJwtException(ExpiredJwtException ex) {
        log.debug("JWT expired", ex);
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "JWT token has expired. Please login again.");
    }

    // Spring Security authentication errors -> Return 401
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        log.debug("Authentication failed", ex);
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, MSG_UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.debug("Access denied", ex);
        return buildErrorResponse(HttpStatus.FORBIDDEN, MSG_FORBIDDEN);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Validation failed")
                .orElse("Validation failed");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(message));
    }

    // 1. Handle errors due to business logic (Example: Duplicate code, BOM loop error) -> Return 400
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequestException(RuntimeException ex) {
        log.debug("Business rule validation failed", ex);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 2. Handle errors due to data not found -> Return 404
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        log.error("Unhandled runtime exception", ex);
        if (ex.getMessage() != null && ex.getMessage().contains("not found")) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, MSG_NOT_FOUND);
        }
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, MSG_INTERNAL_ERROR);
    }

    // 3. Handle all unforeseen system errors -> Return 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception ex) {
        log.error("Unhandled system exception", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, MSG_INTERNAL_ERROR);
    }

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.failure(message));
    }
}
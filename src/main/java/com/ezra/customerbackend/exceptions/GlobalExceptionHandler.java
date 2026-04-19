package com.ezra.customerbackend.exceptions;

import com.ezra.customerbackend.web.response.ApiResponse;
import com.ezra.customerbackend.web.response.ErrorBody;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Maps exceptions to {@link ApiResponse} with {@link ErrorBody} in {@code data} (no stack traces).
 */
@Slf4j
@Order(-2)
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomerException.class)
    public Mono<ResponseEntity<ApiResponse<ErrorBody>>> handleResponseStatus(CustomerException ex,
                                                                             ServerWebExchange exchange) {
        log.debug("Handling response status exception: {}", ex.getMessage());
        HttpStatus status = resolveHttpStatus(ex.getStatus());
        String message = ex.getMessage();
        log.debug("Status code: {}", status);
        if (message == null || message.isBlank()) {
            message = status.getReasonPhrase();
        }
        String code = refineBusinessCode(status, message);
        log.debug("HTTP {} [{}] — {}", status.value(), code, message);
        ErrorBody body = errorBody(exchange, code, null);
        log.debug("Error body: {}", body);
        return Mono.just(ResponseEntity.status(status).body(ApiResponse.of(status.value(), message, body)));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ApiResponse<ErrorBody>>> handleValidation(WebExchangeBindException ex,
                                                                         ServerWebExchange exchange) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        List<ErrorBody.FieldViolation> violations = ex.getBindingResult().getAllErrors().stream()
                .map(err -> {
                    String field = err instanceof org.springframework.validation.FieldError fe
                            ? fe.getField()
                            : err.getObjectName();
                    String msg = err.getDefaultMessage() != null ? err.getDefaultMessage() : err.getCode();
                    return new ErrorBody.FieldViolation(field, msg);
                })
                .toList();
        String message = "Validation failed";
        log.debug("Validation failed: {}", violations);
        ErrorBody body = errorBody(exchange, "VALIDATION_FAILED", violations);
        log.debug("Error body: {}", body);
        return Mono.just(ResponseEntity.status(status).body(ApiResponse.of(status.value(), message, body)));
    }

    @ExceptionHandler(UnsupportedMediaTypeStatusException.class)
    public Mono<ResponseEntity<ApiResponse<ErrorBody>>> handleUnsupportedMedia(
            UnsupportedMediaTypeStatusException ex,
            ServerWebExchange exchange) {
        HttpStatus status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        String message = ex.getReason() != null ? ex.getReason() : "Unsupported media type";
        log.debug("Unsupported media type: {}", message);
        ErrorBody body = errorBody(exchange, "UNSUPPORTED_MEDIA_TYPE", null);
        log.debug("Error body: {}", body);
        return Mono.just(ResponseEntity.status(status).body(ApiResponse.of(status.value(), message, body)));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiResponse<ErrorBody>>> handleUnhandled(Exception ex, ServerWebExchange exchange) {
        log.error("Unhandled error on {}", exchange.getRequest().getPath(), ex);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "An unexpected error occurred. Please try again later.";
        ErrorBody body = errorBody(exchange, "INTERNAL_ERROR", null);
        log.debug("Error body: {}", body);
        return Mono.just(ResponseEntity.status(status).body(ApiResponse.of(status.value(), message, body)));
    }

    private static ErrorBody errorBody(ServerWebExchange exchange, String code,
                                       @Nullable List<ErrorBody.FieldViolation> violations) {
        return ErrorBody.of(code, Instant.now(), exchange.getRequest().getPath().value(), violations);
    }

    private static HttpStatus resolveHttpStatus(HttpStatusCode code) {
        if (code instanceof HttpStatus hs) {
            return hs;
        }
        HttpStatus resolved = HttpStatus.resolve(code.value());
        log.debug("Resolved HTTP status: {}", resolved);
        return resolved != null ? resolved : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static String refineBusinessCode(HttpStatus status, String message) {
        if (status == HttpStatus.NOT_FOUND && message != null) {
            String m = message.toLowerCase();
            log.debug("Message: {}", m);
            if (m.contains("customer")) {
                return "CUSTOMER_NOT_FOUND";
            }
        }
        return httpStatusCode(status);
    }

    private static String httpStatusCode(HttpStatus status) {
        log.debug("Status: {}", status);
        return switch (status) {
            case BAD_REQUEST -> "BAD_REQUEST";
            case UNAUTHORIZED -> "UNAUTHORIZED";
            case FORBIDDEN -> "FORBIDDEN";
            case NOT_FOUND -> "NOT_FOUND";
            case CONFLICT -> "CONFLICT";
            case UNSUPPORTED_MEDIA_TYPE -> "UNSUPPORTED_MEDIA_TYPE";
            case INTERNAL_SERVER_ERROR -> "INTERNAL_SERVER_ERROR";
            case SERVICE_UNAVAILABLE -> "SERVICE_UNAVAILABLE";
            default -> "HTTP_" + status.value();
        };
    }
}

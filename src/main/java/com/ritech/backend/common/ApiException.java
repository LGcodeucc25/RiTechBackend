package com.ritech.backend.common;

import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * An error with a Spanish user-facing message that the handler returns as-is.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final List<FieldErrorDto> fieldErrors;

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, List.of());
    }

    public ApiException(HttpStatus status, String code, String message, List<FieldErrorDto> fieldErrors) {
        super(message);
        this.status = status;
        this.code = code;
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static ApiException validation(List<FieldErrorDto> fieldErrors) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Revisa los datos ingresados.", fieldErrors);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public List<FieldErrorDto> getFieldErrors() {
        return fieldErrors;
    }

    public ApiError toApiError() {
        return new ApiError(code, getMessage(), fieldErrors);
    }
}

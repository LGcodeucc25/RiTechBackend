package com.ritech.backend.common;

import java.util.List;

public record ApiError(String code, String message, List<FieldErrorDto> fieldErrors) {

    public ApiError(String code, String message) {
        this(code, message, List.of());
    }
}

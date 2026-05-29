package com.likelion.backend.global.error;

import java.util.List;
import lombok.Getter;

@Getter
public class ErrorResponse {

    private final boolean success = false;
    private final String message;
    private final String code;
    private final List<FieldErrorResponse> errors;

    private ErrorResponse(String message, String code, List<FieldErrorResponse> errors) {
        this.message = message;
        this.code = code;
        this.errors = errors;
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getMessage(), errorCode.name(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldErrorResponse> errors) {
        return new ErrorResponse(errorCode.getMessage(), errorCode.name(), errors);
    }
}

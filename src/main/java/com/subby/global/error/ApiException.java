package com.subby.global.error;

import java.util.List;

public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<FieldErrorDetail> fieldErrors;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, errorCode.message(), List.of());
    }

    private ApiException(ErrorCode errorCode, String message, List<FieldErrorDetail> fieldErrors) {
        super(message);
        this.errorCode = errorCode;
        this.fieldErrors = fieldErrors;
    }

    /**
     * 특정 입력 항목이 잘못됐을 때. 화면은 errors 배열을 보고 해당 입력칸 아래에 메시지를 보여준다.
     */
    public static ApiException invalidField(String field, String message) {
        return new ApiException(ErrorCode.INVALID_INPUT, message, List.of(new FieldErrorDetail(field, message)));
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public List<FieldErrorDetail> getFieldErrors() {
        return fieldErrors;
    }

    public record FieldErrorDetail(String field, String message) {
    }
}

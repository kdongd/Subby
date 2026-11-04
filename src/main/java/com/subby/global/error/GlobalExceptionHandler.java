package com.subby.global.error;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiException(ApiException e) {
        ErrorCode code = e.getErrorCode();
        ProblemDetail body = ProblemDetails.of(code.status(), code.name(), e.getMessage(), e.getFieldErrors());
        return ResponseEntity.status(code.status()).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = "'" + e.getName() + "' 값의 형식이 올바르지 않습니다.";
        ProblemDetail body = ProblemDetails.of(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_INPUT.name(), message,
                List.of(new ApiException.FieldErrorDetail(e.getName(), message)));
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception e) {
        log.error("처리하지 못한 예외", e);
        return ResponseEntity.internalServerError().body(ProblemDetails.of(ErrorCode.INTERNAL_ERROR));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<ApiException.FieldErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiException.FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList();
        ProblemDetail body = ProblemDetails.of(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_INPUT.name(),
                ErrorCode.INVALID_INPUT.message(), errors);
        return ResponseEntity.badRequest().body(body);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail body = ProblemDetails.of(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_INPUT.name(),
                "요청 형식이 올바르지 않습니다. 날짜와 선택 항목 값을 확인해 주세요.", List.of());
        return ResponseEntity.badRequest().body(body);
    }
}

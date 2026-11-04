package com.subby.global.error;

import java.util.List;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/**
 * 모든 에러 응답을 RFC 9457 ProblemDetail 형식으로 만든다. code는 화면이 분기할 때, detail은 사용자에게 보여줄 때 쓴다.
 */
public final class ProblemDetails {

    private ProblemDetails() {
    }

    public static ProblemDetail of(ErrorCode code) {
        return of(code.status(), code.name(), code.message(), List.of());
    }

    public static ProblemDetail of(HttpStatusCode status, String code, String detail,
                                   List<ApiException.FieldErrorDetail> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(code);
        problem.setProperty("code", code);
        if (!errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }
        return problem;
    }
}

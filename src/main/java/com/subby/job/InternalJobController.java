package com.subby.job;

import com.subby.global.config.AppProperties;
import com.subby.global.error.ErrorCode;
import com.subby.global.error.ProblemDetails;
import com.subby.global.security.SecureTokens;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * GitHub Actions 예약 실행이 매일 호출한다. 서버 안의 스케줄러를 쓰지 않는 이유:
 * Render 무료 서버는 15분 동안 요청이 없으면 꺼지고, 꺼져 있는 동안에는 스케줄러도 돌지 않는다.
 * 외부에서 호출하면 서버를 깨우는 것과 작업 실행이 한 번에 된다.
 */
@RestController
@RequiredArgsConstructor
public class InternalJobController {

    static final String TOKEN_HEADER = "X-Job-Token";

    private final DailyJobService dailyJobService;
    private final AppProperties properties;

    @PostMapping("/internal/jobs/daily")
    public ResponseEntity<?> runDaily(@RequestHeader(value = TOKEN_HEADER, required = false) String token) {
        String expected = properties.jobToken();
        if (!StringUtils.hasText(expected) || !SecureTokens.constantTimeEquals(expected, token)) {
            return ResponseEntity.status(ErrorCode.FORBIDDEN.status()).body(ProblemDetails.of(ErrorCode.FORBIDDEN));
        }
        return ResponseEntity.ok(dailyJobService.run());
    }
}

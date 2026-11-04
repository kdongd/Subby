package com.subby.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.subby.support.IntegrationTestSupport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InternalJobIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("토큰이 없거나 틀리면 예약 작업을 실행하지 않는다")
    void rejectsWithoutToken() throws Exception {
        mockMvc.perform(post("/internal/jobs/daily"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/internal/jobs/daily").header("X-Job-Token", "wrong"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("올바른 토큰이면 환율을 저장하고 알림 작업을 실행한다")
    void runsDailyJob() throws Exception {
        LocalDate day = LocalDate.of(2026, 11, 2);
        when(exchangeRateClient.fetchUsdKrw(any(), any())).thenReturn(Map.of(day, new BigDecimal("1390.25")));

        mockMvc.perform(post("/internal/jobs/daily").header("X-Job-Token", "test-job-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratesSaved").value(1))
                .andExpect(jsonPath("$.usersNotified").value(0));
    }

    @Test
    @DisplayName("환율 API가 실패해도 알림 작업은 계속 진행한다")
    void continuesWhenRateFetchFails() throws Exception {
        when(exchangeRateClient.fetchUsdKrw(any(), any())).thenThrow(new IllegalStateException("timeout"));

        mockMvc.perform(post("/internal/jobs/daily").header("X-Job-Token", "test-job-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratesSaved").value(0))
                .andExpect(jsonPath("$.rateError").value("timeout"));
    }
}

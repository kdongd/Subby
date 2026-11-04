package com.subby.subscription;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.subby.support.IntegrationTestSupport;
import com.subby.user.User;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SubscriptionIntegrationTest extends IntegrationTestSupport {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Seoul"));

    private Map<String, Object> request(String name, String currency, String amount) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("category", "OTT");
        body.put("currency", currency);
        body.put("amount", amount);
        body.put("billingCycle", "MONTHLY");
        body.put("startDate", TODAY.minusMonths(1).toString());
        body.put("memo", "가족 요금제");
        return body;
    }

    private long create(User user, Map<String, Object> body) throws Exception {
        String response = mockMvc.perform(post("/api/subscriptions").with(loginAs(user)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("id").asLong();
    }

    @Test
    @DisplayName("구독을 등록하면 201과 Location 헤더, 다음 결제일을 돌려준다")
    void createAndGet() throws Exception {
        User user = createUser("owner@example.com");

        mockMvc.perform(post("/api/subscriptions").with(loginAs(user)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("넷플릭스", "KRW", "17000"))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("넷플릭스"))
                .andExpect(jsonPath("$.amount").value(17000))
                .andExpect(jsonPath("$.amountKrw").value(17000))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.nextPaymentDate").exists())
                .andExpect(jsonPath("$.upcomingPayments.length()").value(3));
    }

    @Test
    @DisplayName("달러 구독은 소수 둘째 자리까지 받고, 원화 환산액을 함께 준다")
    void usdSubscription() throws Exception {
        User user = createUser("usd@example.com");

        mockMvc.perform(post("/api/subscriptions").with(loginAs(user)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("ChatGPT Plus", "USD", "20.00"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.amount").value(20.00))
                // 환율을 아직 받지 않았으므로 기본 환율(1,400원)로 환산
                .andExpect(jsonPath("$.amountKrw").value(28000));
    }

    @Test
    @DisplayName("원화에 소수점, 달러에 셋째 자리 소수는 거부한다")
    void invalidAmountScale() throws Exception {
        User user = createUser("scale@example.com");

        mockMvc.perform(post("/api/subscriptions").with(loginAs(user)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("원화 소수", "KRW", "1000.5"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("amount"));

        mockMvc.perform(post("/api/subscriptions").with(loginAs(user)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("달러 셋째 자리", "USD", "9.999"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("다른 사람의 구독은 조회·수정·삭제·해지 모두 404")
    void otherUsersSubscriptionIsNotFound() throws Exception {
        User owner = createUser("owner2@example.com");
        User stranger = createUser("stranger@example.com");
        long id = create(owner, request("유튜브 프리미엄", "KRW", "14900"));

        mockMvc.perform(get("/api/subscriptions/" + id).with(loginAs(stranger)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SUBSCRIPTION_NOT_FOUND"));
        mockMvc.perform(put("/api/subscriptions/" + id).with(loginAs(stranger)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("탈취", "KRW", "1"))))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/subscriptions/" + id + "/cancel").with(loginAs(stranger)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("canceledAt", TODAY.toString()))))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/subscriptions/" + id).with(loginAs(stranger)).with(csrf()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/subscriptions/" + id).with(loginAs(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("유튜브 프리미엄"));
        mockMvc.perform(get("/api/subscriptions").with(loginAs(stranger)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("수정, 해지, 삭제")
    void updateCancelDelete() throws Exception {
        User user = createUser("crud@example.com");
        long id = create(user, request("멜론", "KRW", "10900"));

        mockMvc.perform(put("/api/subscriptions/" + id).with(loginAs(user)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request("멜론 무제한", "KRW", "11900"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("멜론 무제한"))
                .andExpect(jsonPath("$.amount").value(11900));

        mockMvc.perform(post("/api/subscriptions/" + id + "/cancel").with(loginAs(user)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("canceledAt", TODAY.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.nextPaymentDate").doesNotExist());

        mockMvc.perform(post("/api/subscriptions/" + id + "/cancel").with(loginAs(user)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("canceledAt", TODAY.toString()))))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/subscriptions").param("status", "ACTIVE").with(loginAs(user)))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/subscriptions").param("status", "CANCELED").with(loginAs(user)))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(delete("/api/subscriptions/" + id).with(loginAs(user)).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/subscriptions/" + id).with(loginAs(user)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("해지일이 첫 결제일보다 빠르면 거부한다")
    void cancelBeforeStart() throws Exception {
        User user = createUser("early@example.com");
        long id = create(user, request("왓챠", "KRW", "7900"));

        mockMvc.perform(post("/api/subscriptions/" + id + "/cancel").with(loginAs(user)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("canceledAt", TODAY.minusYears(1).toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("canceledAt"));
    }

    @Test
    @DisplayName("대시보드는 이번 달 결제 예정액과 7일 내 결제를 보여준다")
    void dashboard() throws Exception {
        User user = createUser("dash@example.com");
        Map<String, Object> body = request("디즈니플러스", "KRW", "9900");
        body.put("startDate", TODAY.toString());
        create(user, body);

        mockMvc.perform(get("/api/dashboard").with(loginAs(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeCount").value(1))
                .andExpect(jsonPath("$.thisMonthTotal").value(9900))
                .andExpect(jsonPath("$.thisMonthRemaining").value(9900))
                .andExpect(jsonPath("$.monthlyAverage").value(9900))
                .andExpect(jsonPath("$.upcoming[0].name").value("디즈니플러스"))
                .andExpect(jsonPath("$.upcoming[0].dDay").value(0))
                .andExpect(jsonPath("$.categories[0].category").value("OTT"))
                .andExpect(jsonPath("$.exchangeRate.fallback").value(true));

        mockMvc.perform(get("/api/stats/monthly").param("months", "6").with(loginAs(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[5].totalKrw").value(9900));
    }
}

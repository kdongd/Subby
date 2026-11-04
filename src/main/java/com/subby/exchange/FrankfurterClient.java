package com.subby.exchange;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.subby.global.config.AppProperties;
import com.subby.global.config.ExternalHttp;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Frankfurter API (유럽중앙은행 기준 환율, 무료, 키 불필요). 영업일 기준 하루 1번 갱신된다.
 * 기간 조회: GET /v1/{시작일}..{종료일}?base=USD&symbols=KRW
 * 응답 예: {"base":"USD","start_date":"...","end_date":"...","rates":{"2026-11-02":{"KRW":1380.25}}}
 */
@Component
public class FrankfurterClient implements ExchangeRateClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public FrankfurterClient(AppProperties properties, ObjectMapper objectMapper) {
        this.restClient = ExternalHttp.client(properties.exchange().baseUrl());
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<LocalDate, BigDecimal> fetchUsdKrw(LocalDate from, LocalDate to) {
        String body = restClient.get()
                .uri("/v1/{range}?base=USD&symbols=KRW", from + ".." + to)
                .retrieve()
                .body(String.class);
        return parse(objectMapper, body);
    }

    static Map<LocalDate, BigDecimal> parse(ObjectMapper objectMapper, String body) {
        Map<LocalDate, BigDecimal> result = new TreeMap<>();
        if (body == null || body.isBlank()) {
            return result;
        }
        try {
            JsonNode rates = objectMapper.readTree(body).path("rates");
            for (Map.Entry<String, JsonNode> entry : rates.properties()) {
                JsonNode krw = entry.getValue().path("KRW");
                if (krw.isNumber()) {
                    result.put(LocalDate.parse(entry.getKey()), krw.decimalValue());
                }
            }
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("환율 응답을 해석하지 못했습니다.", e);
        }
    }
}

package com.subby.global.config;

import java.time.Duration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 외부 API(환율, 메일) 호출용 RestClient. 응답이 없을 때 요청이 무한정 기다리지 않도록 제한 시간을 둔다.
 */
public final class ExternalHttp {

    private ExternalHttp() {
    }

    public static RestClient client(String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(20));
        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .build();
    }
}

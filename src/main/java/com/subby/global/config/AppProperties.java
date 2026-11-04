package com.subby.global.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String baseUrl,
        String zone,
        String jobToken,
        Mail mail,
        Exchange exchange,
        Push push
) {

    public record Mail(String brevoApiKey, String from, String fromName) {
    }

    public record Exchange(String baseUrl, BigDecimal fallbackUsdKrw, boolean fetchOnStartup, int initialMonths) {
    }

    /**
     * 웹 푸시 VAPID 키. {@code npx web-push generate-vapid-keys}로 만든 값을 그대로 넣는다.
     *
     * @param publicKey  공개키 (base64url, 65바이트). 브라우저에도 내려간다.
     * @param privateKey 개인키 (base64url, 32바이트). 절대 공개하지 않는다.
     * @param subject    푸시 서버가 문제 있을 때 연락할 주소. mailto: 또는 https://. 비우면 base-url을 쓴다.
     */
    public record Push(String publicKey, String privateKey, String subject) {
    }
}

package com.subby.global.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String baseUrl,
        String zone,
        String jobToken,
        String unsubscribeSecret,
        Mail mail,
        Exchange exchange
) {

    public record Mail(String brevoApiKey, String from, String fromName) {
    }

    public record Exchange(String baseUrl, BigDecimal fallbackUsdKrw, boolean fetchOnStartup, int initialMonths) {
    }
}

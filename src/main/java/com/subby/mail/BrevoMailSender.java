package com.subby.mail;

import com.subby.global.config.ExternalHttp;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Brevo HTTP API로 메일을 보낸다.
 * Render 무료 서버는 SMTP 포트(25, 465, 587)가 막혀 있어서 일반 SMTP 메일 설정은 배포 후 동작하지 않는다.
 */
public class BrevoMailSender implements MailSender {

    private static final String BASE_URL = "https://api.brevo.com";

    private final RestClient restClient;
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;

    public BrevoMailSender(String apiKey, String fromEmail, String fromName) {
        this.restClient = ExternalHttp.client(BASE_URL);
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    @Override
    public void send(String to, String subject, String html) {
        Map<String, Object> body = Map.of(
                "sender", Map.of("name", fromName, "email", fromEmail),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "htmlContent", html);
        try {
            restClient.post()
                    .uri("/v3/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new MailSendException("Brevo 메일 발송 실패", e);
        }
    }
}

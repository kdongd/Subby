package com.subby.mail;

import com.subby.global.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
public class MailConfig {

    @Bean
    public MailSender mailSender(AppProperties properties) {
        AppProperties.Mail mail = properties.mail();
        if (!StringUtils.hasText(mail.brevoApiKey()) || !StringUtils.hasText(mail.from())) {
            log.warn("BREVO_API_KEY 또는 MAIL_FROM이 없어 메일을 실제로 보내지 않고 로그로만 출력합니다.");
            return new LoggingMailSender();
        }
        return new BrevoMailSender(mail.brevoApiKey(), mail.from(), mail.fromName());
    }
}

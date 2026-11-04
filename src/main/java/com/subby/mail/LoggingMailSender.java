package com.subby.mail;

import lombok.extern.slf4j.Slf4j;

/**
 * 로컬 개발용. Brevo 키가 없으면 메일을 보내지 않고 내용을 로그로 출력한다.
 * 비밀번호 재설정 링크도 로그에서 확인할 수 있다.
 */
@Slf4j
public class LoggingMailSender implements MailSender {

    @Override
    public void send(String to, String subject, String html) {
        log.info("[메일 발송 생략 - BREVO_API_KEY 없음] 받는 사람: {}, 제목: {}\n{}", to, subject, html);
    }
}

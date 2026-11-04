package com.subby.mail;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * 메일 본문 HTML. 지금은 비밀번호 재설정 메일만 보낸다(결제 알림은 웹 푸시 + 사이트 안 알림).
 * 사용자가 입력한 값(닉네임, 서비스명)은 반드시 이스케이프해서 HTML로 해석되지 않게 한다.
 */
@Component
public class MailTemplates {

    public String passwordResetSubject() {
        return "[서비] 비밀번호 재설정 안내";
    }

    public String passwordReset(String nickname, String link) {
        return layout("""
                <p>%s님, 비밀번호 재설정을 요청하셨습니다.</p>
                <p>아래 버튼을 눌러 새 비밀번호를 설정해 주세요. 링크는 <b>30분 동안 한 번만</b> 사용할 수 있습니다.</p>
                <p style="margin:24px 0"><a href="%s" style="background:#2f6fde;color:#fff;padding:12px 20px;border-radius:8px;text-decoration:none">비밀번호 재설정</a></p>
                <p style="color:#666;font-size:13px">요청하지 않으셨다면 이 메일을 무시하셔도 됩니다. 비밀번호는 바뀌지 않습니다.</p>
                """.formatted(escape(nickname), escape(link)));
    }

    private String layout(String content) {
        return """
                <div style="font-family:-apple-system,'Apple SD Gothic Neo','Malgun Gothic',sans-serif;max-width:560px;margin:0 auto;padding:24px;color:#222;line-height:1.6">
                  <h2 style="margin:0 0 16px">서비</h2>
                  %s
                </div>
                """.formatted(content);
    }

    private static String escape(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }
}

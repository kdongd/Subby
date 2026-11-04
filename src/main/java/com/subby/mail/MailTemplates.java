package com.subby.mail;

import com.subby.exchange.MoneyFormat;
import com.subby.notification.DuePayment;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * 메일 본문 HTML. 사용자가 입력한 값(닉네임, 서비스명)은 반드시 이스케이프해서 HTML로 해석되지 않게 한다.
 */
@Component
public class MailTemplates {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN);

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

    public String paymentReminderSubject(int count) {
        return "[서비] 곧 결제될 구독 " + count + "건";
    }

    public String paymentReminder(String nickname, List<DuePayment> payments, String unsubscribeLink) {
        StringBuilder rows = new StringBuilder();
        for (DuePayment payment : payments) {
            rows.append("""
                    <tr>
                      <td style="padding:8px 12px;border-bottom:1px solid #eee">%s</td>
                      <td style="padding:8px 12px;border-bottom:1px solid #eee">%s</td>
                      <td style="padding:8px 12px;border-bottom:1px solid #eee;text-align:right">%s</td>
                    </tr>
                    """.formatted(
                    escape(payment.name()),
                    DATE.format(payment.paymentDate()),
                    escape(MoneyFormat.withKrw(payment.currency(), payment.amount(), payment.amountKrw()))));
        }
        return layout("""
                <p>%s님, 곧 결제될 구독이 있습니다.</p>
                <table style="border-collapse:collapse;width:100%%;font-size:14px">
                  <tr style="background:#f6f7f9">
                    <th style="padding:8px 12px;text-align:left">서비스</th>
                    <th style="padding:8px 12px;text-align:left">결제일</th>
                    <th style="padding:8px 12px;text-align:right">금액</th>
                  </tr>
                  %s
                </table>
                <p style="color:#666;font-size:13px">달러 결제의 원화 금액은 환율 기준 추정치이며 실제 카드 청구액과 다를 수 있습니다.</p>
                <p style="color:#999;font-size:12px;margin-top:24px">알림을 그만 받으려면 <a href="%s" style="color:#999">여기</a>를 눌러 주세요.</p>
                """.formatted(escape(nickname), rows, escape(unsubscribeLink)));
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

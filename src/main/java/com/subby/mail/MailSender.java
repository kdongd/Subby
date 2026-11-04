package com.subby.mail;

public interface MailSender {

    /** 메일을 보낸다. 실패하면 MailSendException을 던진다. */
    void send(String to, String subject, String html);
}

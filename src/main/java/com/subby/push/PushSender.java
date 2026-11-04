package com.subby.push;

/** 푸시 서버로 메시지 1개를 보낸다. 테스트에서는 가짜로 바꾼다. */
public interface PushSender {

    /** 서버에 VAPID 키가 설정돼 있어 실제로 보낼 수 있는지 */
    boolean isEnabled();

    /** 브라우저가 구독할 때 쓰는 공개키. 사용할 수 없으면 null */
    String publicKey();

    SendResult send(PushSubscription subscription, byte[] payload);

    enum SendResult {
        /** 푸시 서버가 받았다 */
        SENT,
        /** 구독이 만료되거나 해제됐다. 저장된 구독을 지워야 한다 */
        GONE,
        /** 일시적인 실패. 구독은 그대로 둔다 */
        FAILED,
        /** 서버에 키가 없어 보내지 않았다 */
        DISABLED
    }
}

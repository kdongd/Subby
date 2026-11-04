package com.subby.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WebPushCryptoTest {

    /**
     * npm의 http_ece(웹 푸시 라이브러리들이 쓰는 표준 구현)로 같은 키·salt를 넣어 만든 결과.
     * 바이트 단위로 같아야 브라우저가 복호화할 수 있다.
     */
    private static final String UA_PUBLIC = "BAvDis_3Rfel4GDVEBAvizXtKa12by0qtGT3UCYttSjmC79pkoyduVOlF8tDOR2W8N6Jl-kkggRsUQacIparIes";
    private static final String AS_PRIVATE = "I7QGPMRHHmkfZy07HGE6myvxaMawvNbhIgsjqPvNO3A";
    private static final String AS_PUBLIC = "BKGR8EKoEbbbpzw-wCN6nrqRhvOjbiiPjmZKJ-comTfRDP6UxspE3nTm5Zz8vQsNKGhSYwz-CqX-fS-25JCQnIw";
    private static final String AUTH = "OSzrT-PR-1311pngWy9vzA";
    private static final String SALT = "K5k0kEE5ZorpqFQkwaW4BQ";
    private static final String EXPECTED_BODY = "K5k0kEE5ZorpqFQkwaW4BQAAEABBBKGR8EKoEbbbpzw-wCN6nrqRhvOjbiiPjmZKJ-comTfRDP6UxspE3nTm5Zz8vQsNKGhSYwz-CqX-fS-25JCQnIwe7p9QqwFdZhtIJnb-EnDq6h-jjPz-jktXcq2xYQKqdK7k1Ss7oU3clrD1IP8lvaVliPOVdS7r";

    @Test
    @DisplayName("표준 구현(http_ece)과 같은 입력이면 같은 암호문을 만든다")
    void matchesReferenceImplementation() throws Exception {
        KeyPair ephemeral = new KeyPair(
                WebPushCrypto.decodePublicKey(b64(AS_PUBLIC)),
                WebPushCrypto.decodePrivateKey(b64(AS_PRIVATE)));

        byte[] body = WebPushCrypto.encrypt("{\"title\":\"넷플릭스 결제 예정\"}".getBytes(StandardCharsets.UTF_8),
                b64(UA_PUBLIC), b64(AUTH), ephemeral, b64(SALT));

        assertThat(WebPushCrypto.b64(body)).isEqualTo(EXPECTED_BODY);
    }

    @Test
    @DisplayName("브라우저 쪽 키로 복호화하면 원래 내용이 나온다")
    void roundTrip() throws Exception {
        KeyPair browser = newKeyPair();
        byte[] auth = new byte[16];
        new java.security.SecureRandom().nextBytes(auth);
        byte[] uaPublic = WebPushCrypto.encodePublicKey((ECPublicKey) browser.getPublic());
        String message = "{\"title\":\"곧 결제될 구독 3건\",\"url\":\"/notifications\"}";

        byte[] body = WebPushCrypto.encrypt(message.getBytes(StandardCharsets.UTF_8), uaPublic, auth);

        assertThat(new String(decrypt(body, browser, uaPublic, auth), StandardCharsets.UTF_8)).isEqualTo(message);
    }

    @Test
    @DisplayName("구독 키 형식이 틀리면 거부한다")
    void rejectsInvalidKeys() {
        assertThatThrownBy(() -> WebPushCrypto.encrypt(new byte[1], new byte[64], new byte[16]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WebPushCrypto.encrypt(new byte[1], b64(UA_PUBLIC), new byte[8]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("VAPID 헤더는 공개키로 검증되는 ES256 JWT를 담는다")
    void vapidJwt() throws Exception {
        String publicKey = "BD-9NlMan8RL2CQidKYPEVrW2niC2jSUD8yYtDrQ9GzhT4JzIbcvxgdT7N9CFkPcHFI1CU726dwa6IDGuHsMKh8";
        String privateKey = "T1NuwKrrhifc49lizKjSVK70-ssL6QZCZrZgsR-OHOE";

        String header = WebPushCrypto.vapidAuthorization("https://fcm.googleapis.com", "mailto:test@example.com",
                1_800_000_000L, publicKey, privateKey);

        assertThat(header).startsWith("vapid t=").endsWith(", k=" + publicKey);
        String jwt = header.substring("vapid t=".length(), header.indexOf(", k="));
        String[] parts = jwt.split("\\.");
        assertThat(new String(b64(parts[1]), StandardCharsets.UTF_8))
                .isEqualTo("{\"aud\":\"https://fcm.googleapis.com\",\"exp\":1800000000,\"sub\":\"mailto:test@example.com\"}");
        Signature verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
        verifier.initVerify(WebPushCrypto.decodePublicKey(b64(publicKey)));
        verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        assertThat(verifier.verify(b64(parts[2]))).isTrue();

        assertThat(WebPushCrypto.isKeyPair(publicKey, privateKey)).isTrue();
        assertThat(WebPushCrypto.isKeyPair(AS_PUBLIC, privateKey)).isFalse();
        assertThat(WebPushCrypto.isKeyPair("broken", privateKey)).isFalse();
    }

    /** 브라우저가 하는 일(RFC 8291 수신 측)을 그대로 따라 한다. */
    private static byte[] decrypt(byte[] body, KeyPair browser, byte[] uaPublic, byte[] auth) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(body);
        byte[] salt = new byte[16];
        buffer.get(salt);
        assertThat(buffer.getInt()).isEqualTo(4096);
        byte[] asPublic = new byte[buffer.get()];
        buffer.get(asPublic);
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
        agreement.init(browser.getPrivate());
        agreement.doPhase(WebPushCrypto.decodePublicKey(asPublic), true);
        byte[] ecdh = agreement.generateSecret();

        byte[] info = concat("WebPush: info\0".getBytes(StandardCharsets.US_ASCII), uaPublic, asPublic);
        byte[] ikm = expand(hmac(auth, ecdh), info, 32);
        byte[] prk = hmac(salt, ikm);
        byte[] cek = expand(prk, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), 16);
        byte[] nonce = expand(prk, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), 12);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] padded = cipher.doFinal(ciphertext);
        assertThat(padded[padded.length - 1]).isEqualTo((byte) 0x02);
        return Arrays.copyOf(padded, padded.length - 1);
    }

    private static KeyPair newKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    private static byte[] expand(byte[] prk, byte[] info, int length) throws Exception {
        return Arrays.copyOf(hmac(prk, concat(info, new byte[] {1})), length);
    }

    private static byte[] hmac(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static byte[] concat(byte[]... parts) {
        int length = Arrays.stream(parts).mapToInt(p -> p.length).sum();
        ByteBuffer out = ByteBuffer.allocate(length);
        for (byte[] part : parts) {
            out.put(part);
        }
        return out.array();
    }

    private static byte[] b64(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}

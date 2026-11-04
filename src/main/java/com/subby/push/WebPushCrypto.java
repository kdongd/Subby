package com.subby.push;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 웹 푸시 메시지 암호화(RFC 8291, aes128gcm)와 발신자 인증(VAPID, RFC 8292).
 * 외부 라이브러리 없이 JDK 암호화 기능만 쓴다.
 *
 * <p>푸시 메시지는 Google·Apple·Mozilla의 푸시 서버를 거쳐 전달되는데, 그 서버들은 내용을 볼 수 없어야 한다.
 * 그래서 브라우저가 구독할 때 준 공개키(p256dh)와 비밀값(auth)으로 메시지를 암호화하고,
 * 푸시 서버에는 "보낸 쪽이 서비가 맞다"는 서명(VAPID JWT)만 함께 보낸다.
 */
public final class WebPushCrypto {

    private static final int RECORD_SIZE = 4096;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Decoder B64_DECODER = Base64.getUrlDecoder();
    private static final Base64.Encoder B64_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final ECParameterSpec P256 = p256();

    private WebPushCrypto() {
    }

    // ---------------------------------------------------------------- 암호화 (RFC 8291)

    /**
     * @param uaPublic   브라우저 공개키 (구독 정보의 p256dh, 65바이트 비압축 형식)
     * @param authSecret 브라우저 비밀값 (구독 정보의 auth, 16바이트)
     * @return 푸시 서버로 보낼 요청 본문 (헤더 + 암호문)
     */
    public static byte[] encrypt(byte[] payload, byte[] uaPublic, byte[] authSecret) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"), RANDOM);
            KeyPair ephemeral = generator.generateKeyPair();
            byte[] salt = new byte[16];
            RANDOM.nextBytes(salt);
            return encrypt(payload, uaPublic, authSecret, ephemeral, salt);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("웹 푸시 암호화 실패", e);
        }
    }

    /** 테스트에서 서버 임시 키와 salt를 고정하기 위해 분리했다. */
    static byte[] encrypt(byte[] payload, byte[] uaPublic, byte[] authSecret, KeyPair ephemeral, byte[] salt)
            throws GeneralSecurityException {
        if (uaPublic.length != 65 || uaPublic[0] != 0x04) {
            throw new IllegalArgumentException("p256dh 형식이 올바르지 않습니다.");
        }
        if (authSecret.length != 16) {
            throw new IllegalArgumentException("auth 형식이 올바르지 않습니다.");
        }
        byte[] asPublic = encodePublicKey((ECPublicKey) ephemeral.getPublic());

        KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
        agreement.init(ephemeral.getPrivate());
        agreement.doPhase(decodePublicKey(uaPublic), true);
        byte[] ecdhSecret = agreement.generateSecret();

        // IKM = HKDF(salt=auth, ikm=ecdh, info="WebPush: info\0" || ua_public || as_public, L=32)
        byte[] keyInfo = concat("WebPush: info\0".getBytes(StandardCharsets.US_ASCII), uaPublic, asPublic);
        byte[] ikm = hkdf(authSecret, ecdhSecret, keyInfo, 32);

        // CEK, NONCE = HKDF(salt, IKM, ...)
        byte[] prk = hmac(salt, ikm);
        byte[] cek = expand(prk, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), 16);
        byte[] nonce = expand(prk, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), 12);

        // 레코드 하나짜리 메시지: 평문 뒤에 마지막 레코드 구분자 0x02
        byte[] plaintext = Arrays.copyOf(payload, payload.length + 1);
        plaintext[payload.length] = 0x02;
        if (plaintext.length + 16 > RECORD_SIZE) {
            throw new IllegalArgumentException("푸시 메시지가 너무 깁니다.");
        }

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] ciphertext = cipher.doFinal(plaintext);

        // 헤더: salt(16) || rs(4) || idlen(1) || keyid(as_public 65)
        ByteBuffer body = ByteBuffer.allocate(16 + 4 + 1 + asPublic.length + ciphertext.length);
        body.put(salt).putInt(RECORD_SIZE).put((byte) asPublic.length).put(asPublic).put(ciphertext);
        return body.array();
    }

    // ---------------------------------------------------------------- VAPID (RFC 8292)

    /**
     * 푸시 서버에 보내는 Authorization 헤더 값. "vapid t=JWT, k=공개키"
     *
     * @param audience 푸시 서버의 origin (예: https://fcm.googleapis.com)
     * @param subject  연락처 (mailto: 또는 https 주소)
     */
    public static String vapidAuthorization(String audience, String subject, long expiresAtEpochSeconds,
                                            String publicKeyB64, String privateKeyB64) {
        try {
            String header = b64(("{\"typ\":\"JWT\",\"alg\":\"ES256\"}").getBytes(StandardCharsets.UTF_8));
            String claims = b64(("{\"aud\":\"" + jsonEscape(audience) + "\",\"exp\":" + expiresAtEpochSeconds
                    + ",\"sub\":\"" + jsonEscape(subject) + "\"}").getBytes(StandardCharsets.UTF_8));
            String signingInput = header + "." + claims;

            // ES256 JWT 서명은 DER가 아니라 r||s 64바이트 형식이어야 한다.
            Signature signer = Signature.getInstance("SHA256withECDSAinP1363Format");
            signer.initSign(decodePrivateKey(b64Decode(privateKeyB64)));
            signer.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            String jwt = signingInput + "." + b64(signer.sign());
            return "vapid t=" + jwt + ", k=" + publicKeyB64;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("VAPID 서명 실패", e);
        }
    }

    /** 공개키와 개인키가 한 쌍인지 확인한다. 설정 실수를 서버 시작 시점에 잡기 위해서다. */
    public static boolean isKeyPair(String publicKeyB64, String privateKeyB64) {
        try {
            byte[] message = "subby-vapid-check".getBytes(StandardCharsets.US_ASCII);
            Signature signer = Signature.getInstance("SHA256withECDSA");
            signer.initSign(decodePrivateKey(b64Decode(privateKeyB64)));
            signer.update(message);
            byte[] signature = signer.sign();
            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(decodePublicKey(b64Decode(publicKeyB64)));
            verifier.update(message);
            return verifier.verify(signature);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    // ---------------------------------------------------------------- 키 변환

    public static ECPublicKey decodePublicKey(byte[] uncompressed) throws GeneralSecurityException {
        if (uncompressed.length != 65 || uncompressed[0] != 0x04) {
            throw new IllegalArgumentException("P-256 공개키 형식이 올바르지 않습니다.");
        }
        BigInteger x = new BigInteger(1, Arrays.copyOfRange(uncompressed, 1, 33));
        BigInteger y = new BigInteger(1, Arrays.copyOfRange(uncompressed, 33, 65));
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(new ECPoint(x, y), P256));
    }

    public static ECPrivateKey decodePrivateKey(byte[] d) throws GeneralSecurityException {
        if (d.length != 32) {
            throw new IllegalArgumentException("P-256 개인키 형식이 올바르지 않습니다.");
        }
        return (ECPrivateKey) KeyFactory.getInstance("EC")
                .generatePrivate(new ECPrivateKeySpec(new BigInteger(1, d), P256));
    }

    public static byte[] encodePublicKey(ECPublicKey key) {
        byte[] out = new byte[65];
        out[0] = 0x04;
        copyUnsigned(key.getW().getAffineX(), out, 1);
        copyUnsigned(key.getW().getAffineY(), out, 33);
        return out;
    }

    public static byte[] b64Decode(String value) {
        return B64_DECODER.decode(value.trim().replace('+', '-').replace('/', '_').replace("=", ""));
    }

    public static String b64(byte[] value) {
        return B64_ENCODER.encodeToString(value);
    }

    // ---------------------------------------------------------------- 내부 도구

    private static ECParameterSpec p256() {
        try {
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256r1"));
            return parameters.getParameterSpec(ECParameterSpec.class);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void copyUnsigned(BigInteger value, byte[] out, int offset) {
        byte[] bytes = value.toByteArray();
        int length = Math.min(bytes.length, 32);
        System.arraycopy(bytes, bytes.length - length, out, offset + 32 - length, length);
    }

    private static byte[] hkdf(byte[] salt, byte[] ikm, byte[] info, int length) throws GeneralSecurityException {
        return expand(hmac(salt, ikm), info, length);
    }

    /** 32바이트 이하만 필요하므로 HKDF-Expand의 첫 블록만 계산한다. */
    private static byte[] expand(byte[] prk, byte[] info, int length) throws GeneralSecurityException {
        return Arrays.copyOf(hmac(prk, concat(info, new byte[] {0x01})), length);
    }

    private static byte[] hmac(byte[] key, byte[] data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static byte[] concat(byte[]... parts) {
        int length = 0;
        for (byte[] part : parts) {
            length += part.length;
        }
        byte[] out = new byte[length];
        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, out, offset, part.length);
            offset += part.length;
        }
        return out;
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

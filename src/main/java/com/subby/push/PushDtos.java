package com.subby.push;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class PushDtos {

    private PushDtos() {
    }

    /** enabled가 false면 서버에 VAPID 키가 없어 기기 푸시를 쓸 수 없다. */
    public record PublicKeyResponse(boolean enabled, String publicKey) {
    }

    /** 브라우저 PushSubscription.toJSON() 결과를 그대로 보낸다. */
    public record SubscribeRequest(
            @NotBlank(message = "푸시 구독 정보가 올바르지 않습니다.")
            @Size(max = PushEndpointPolicy.MAX_LENGTH, message = "푸시 구독 정보가 올바르지 않습니다.")
            String endpoint,
            @NotNull(message = "푸시 구독 정보가 올바르지 않습니다.") @Valid Keys keys
    ) {
    }

    public record Keys(
            @NotBlank(message = "푸시 구독 정보가 올바르지 않습니다.") @Size(max = 100) String p256dh,
            @NotBlank(message = "푸시 구독 정보가 올바르지 않습니다.") @Size(max = 50) String auth
    ) {
    }

    public record EndpointRequest(
            @NotBlank(message = "푸시 구독 정보가 올바르지 않습니다.")
            @Size(max = PushEndpointPolicy.MAX_LENGTH)
            String endpoint
    ) {
    }

    /** 이 기기(endpoint)가 현재 계정으로 구독돼 있는지 */
    public record StatusResponse(boolean subscribed, long deviceCount) {
    }

    /** @param devices 시도한 기기 수, sent 성공 수 */
    public record TestResponse(int devices, int sent) {
    }
}

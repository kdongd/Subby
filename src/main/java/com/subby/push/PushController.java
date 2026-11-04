package com.subby.push;

import com.subby.global.security.UserPrincipal;
import com.subby.push.PushDtos.EndpointRequest;
import com.subby.push.PushDtos.SubscribeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final PushService pushService;

    @GetMapping("/public-key")
    public PushDtos.PublicKeyResponse publicKey() {
        return pushService.publicKey();
    }

    /** endpoint는 길고 URL에 넣기 곤란해서 조회도 POST 본문으로 받는다. */
    @PostMapping("/status")
    public PushDtos.StatusResponse status(@AuthenticationPrincipal UserPrincipal principal,
                                          @RequestBody(required = false) EndpointRequest request) {
        return pushService.status(principal.getId(), request == null ? null : request.endpoint());
    }

    @PostMapping("/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void subscribe(@AuthenticationPrincipal UserPrincipal principal,
                          @Valid @RequestBody SubscribeRequest request,
                          @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        pushService.subscribe(principal.getId(), request.endpoint(), request.keys(), userAgent);
    }

    @PostMapping("/unsubscribe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@AuthenticationPrincipal UserPrincipal principal,
                            @Valid @RequestBody EndpointRequest request) {
        pushService.unsubscribe(principal.getId(), request.endpoint());
    }

    @PostMapping("/test")
    public PushDtos.TestResponse test(@AuthenticationPrincipal UserPrincipal principal) {
        return pushService.sendTest(principal.getId());
    }
}

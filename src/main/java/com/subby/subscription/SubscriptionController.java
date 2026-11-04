package com.subby.subscription;

import com.subby.global.security.UserPrincipal;
import com.subby.subscription.dto.SubscriptionDtos.CancelRequest;
import com.subby.subscription.dto.SubscriptionDtos.SubscriptionRequest;
import com.subby.subscription.dto.SubscriptionDtos.SubscriptionResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    public List<SubscriptionResponse> list(@AuthenticationPrincipal UserPrincipal principal,
                                           @RequestParam(required = false) SubscriptionStatus status,
                                           @RequestParam(required = false) Category category) {
        return subscriptionService.list(principal.getId(), status, category);
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                                       @Valid @RequestBody SubscriptionRequest request) {
        SubscriptionResponse response = subscriptionService.create(principal.getId(), request);
        return ResponseEntity.created(URI.create("/api/subscriptions/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public SubscriptionResponse get(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return subscriptionService.get(principal.getId(), id);
    }

    @PutMapping("/{id}")
    public SubscriptionResponse update(@AuthenticationPrincipal UserPrincipal principal,
                                       @PathVariable Long id,
                                       @Valid @RequestBody SubscriptionRequest request) {
        return subscriptionService.update(principal.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        subscriptionService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel")
    public SubscriptionResponse cancel(@AuthenticationPrincipal UserPrincipal principal,
                                       @PathVariable Long id,
                                       @Valid @RequestBody CancelRequest request) {
        return subscriptionService.cancel(principal.getId(), id, request.canceledAt());
    }
}

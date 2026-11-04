package com.subby.subscription;

import com.subby.exchange.ExchangeRateService;
import com.subby.exchange.RateTable;
import com.subby.global.error.ApiException;
import com.subby.global.error.ErrorCode;
import com.subby.subscription.dto.SubscriptionDtos.SubscriptionRequest;
import com.subby.subscription.dto.SubscriptionDtos.SubscriptionResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final ExchangeRateService exchangeRateService;
    private final Clock clock;

    /** 활성 구독을 다음 결제일이 가까운 순으로, 해지한 구독은 뒤에 둔다. */
    public List<SubscriptionResponse> list(Long userId, SubscriptionStatus status, Category category) {
        LocalDate today = LocalDate.now(clock);
        RateTable rates = exchangeRateService.currentTable();
        return subscriptionRepository.findAllByUserId(userId).stream()
                .filter(s -> status == null || s.getStatus() == status)
                .filter(s -> category == null || s.getCategory() == category)
                .map(s -> SubscriptionResponse.of(s, today, rates))
                .sorted(Comparator
                        .comparing(SubscriptionResponse::nextPaymentDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(SubscriptionResponse::name))
                .toList();
    }

    public SubscriptionResponse get(Long userId, Long id) {
        return toResponse(find(userId, id));
    }

    @Transactional
    public SubscriptionResponse create(Long userId, SubscriptionRequest request) {
        validateAmount(request.currency(), request.amount());
        Subscription subscription = subscriptionRepository.save(Subscription.create(userId, request.toDetails()));
        return toResponse(subscription);
    }

    @Transactional
    public SubscriptionResponse update(Long userId, Long id, SubscriptionRequest request) {
        validateAmount(request.currency(), request.amount());
        Subscription subscription = find(userId, id);
        try {
            subscription.update(request.toDetails());
        } catch (IllegalArgumentException e) {
            throw ApiException.invalidField("startDate", e.getMessage());
        }
        return toResponse(subscription);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        subscriptionRepository.delete(find(userId, id));
    }

    @Transactional
    public SubscriptionResponse cancel(Long userId, Long id, LocalDate canceledAt) {
        Subscription subscription = find(userId, id);
        if (!subscription.isActive()) {
            throw new ApiException(ErrorCode.ALREADY_CANCELED);
        }
        try {
            subscription.cancel(canceledAt);
        } catch (IllegalArgumentException e) {
            throw ApiException.invalidField("canceledAt", e.getMessage());
        }
        return toResponse(subscription);
    }

    /** 원: 정수 1 ~ 10,000,000원. 달러: 소수 둘째 자리까지 0.01 ~ 100,000달러 */
    static void validateAmount(CurrencyCode currency, BigDecimal amount) {
        BigDecimal normalized = amount.stripTrailingZeros();
        if (normalized.scale() > currency.scale()) {
            throw ApiException.invalidField("amount", currency == CurrencyCode.KRW
                    ? "원화 금액은 정수로 입력해 주세요."
                    : "달러 금액은 소수 둘째 자리까지 입력할 수 있습니다.");
        }
        if (amount.compareTo(currency.min()) < 0 || amount.compareTo(currency.max()) > 0) {
            throw ApiException.invalidField("amount", currency == CurrencyCode.KRW
                    ? "원화 금액은 1원 ~ 10,000,000원 사이로 입력해 주세요."
                    : "달러 금액은 $0.01 ~ $100,000 사이로 입력해 주세요.");
        }
    }

    /** 본인 구독이 아니면 존재 여부도 알려주지 않고 404로 응답한다. */
    private Subscription find(Long userId, Long id) {
        return subscriptionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
    }

    private SubscriptionResponse toResponse(Subscription subscription) {
        return SubscriptionResponse.of(subscription, LocalDate.now(clock), exchangeRateService.currentTable());
    }
}

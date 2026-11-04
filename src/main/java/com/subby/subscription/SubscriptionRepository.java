package com.subby.subscription;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 모든 조회는 userId 조건을 함께 건다. 다른 사람의 구독 ID를 넣어도 찾을 수 없게 하기 위해서다.
 */
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByIdAndUserId(Long id, Long userId);

    List<Subscription> findAllByUserId(Long userId);

    List<Subscription> findAllByUserIdAndStatus(Long userId, SubscriptionStatus status);
}

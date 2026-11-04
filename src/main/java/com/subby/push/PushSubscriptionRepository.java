package com.subby.push;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    Optional<PushSubscription> findByEndpoint(String endpoint);

    List<PushSubscription> findAllByUserIdOrderByCreatedAtAscIdAsc(Long userId);

    long countByUserId(Long userId);

    boolean existsByUserIdAndEndpoint(Long userId, String endpoint);

    @Modifying
    @Query("delete from PushSubscription p where p.userId = :userId and p.endpoint = :endpoint")
    int deleteByUserIdAndEndpoint(Long userId, String endpoint);
}

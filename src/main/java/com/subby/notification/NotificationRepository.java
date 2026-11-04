package com.subby.notification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsBySubscriptionIdAndPaymentDate(Long subscriptionId, LocalDate paymentDate);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    List<Notification> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(Long userId);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.userId = :userId and n.readAt is null")
    int markAllRead(Long userId, Instant now);

    @Modifying
    @Query("delete from Notification n where n.createdAt < :before")
    int deleteCreatedBefore(Instant before);
}

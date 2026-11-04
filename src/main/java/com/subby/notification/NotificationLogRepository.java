package com.subby.notification;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    boolean existsBySubscriptionIdAndPaymentDate(Long subscriptionId, LocalDate paymentDate);
}

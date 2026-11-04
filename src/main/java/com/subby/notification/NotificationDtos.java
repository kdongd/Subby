package com.subby.notification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationResponse(
            Long id,
            String title,
            String body,
            String link,
            LocalDate paymentDate,
            boolean read,
            Instant createdAt
    ) {
        static NotificationResponse from(Notification notification) {
            return new NotificationResponse(notification.getId(), notification.getTitle(), notification.getBody(),
                    notification.getLink(), notification.getPaymentDate(), notification.isRead(),
                    notification.getCreatedAt());
        }
    }

    public record NotificationListResponse(List<NotificationResponse> items, long unreadCount) {
    }

    public record UnreadCountResponse(long unreadCount) {
    }
}

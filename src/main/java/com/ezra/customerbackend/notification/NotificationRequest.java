package com.ezra.customerbackend.notification;

import java.time.Instant;
import java.util.List;

public record NotificationRequest(
        String source,
        String eventId,
        Instant eventTime,
        String eventType,
        Customer customer,
        Notification notification
) {
}

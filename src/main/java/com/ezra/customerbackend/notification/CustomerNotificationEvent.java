package com.ezra.customerbackend.notification;

import java.time.Instant;

public record CustomerNotificationEvent(
        String eventType,
        Long customerId,
        String message,
        Instant occurredAt
) {
}

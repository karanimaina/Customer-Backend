package com.ezra.customerbackend.service;

import com.ezra.customerbackend.notification.CustomerNotificationEvent;
import com.ezra.customerbackend.notification.CustomerNotificationPublisher;
import com.ezra.customerbackend.notification.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerNotificationService implements CustomerNotificationPublisher {

    private static final String NOTIFICATION_REQUEST_BINDING = "notification-request-out-0";

    private final StreamBridge streamBridge;
    @Value("${spring.application.name:customer-service}")
    private String applicationName;

    @Override
    public void publish(CustomerNotificationEvent event) {
        NotificationRequest request = toNotificationRequest(event);
        sendAsync(NOTIFICATION_REQUEST_BINDING, request);
    }

    private NotificationRequest toNotificationRequest(CustomerNotificationEvent event) {
        String type = isBlank(event.eventType()) ? "CUSTOMER_EVENT" : event.eventType();
        String message = isBlank(event.message()) ? "Customer event emitted." : event.message();
        Instant eventTime = event.occurredAt() != null ? event.occurredAt() : Instant.now();
        String customerId = event.customerId() != null ? event.customerId().toString() : "UNKNOWN";

        return new NotificationRequest(
                applicationName,
                UUID.randomUUID().toString(),
                eventTime,
                type,
                new NotificationRequest.CustomerPayload(customerId),
                new NotificationRequest.NotificationPayload(message, List.of("EMAIL"))
        );
    }

    private void sendAsync(String bindingName, NotificationRequest event) {
        Mono.fromRunnable(() -> {
                    boolean sent = streamBridge.send(bindingName, event);
                    if (!sent) {
                        log.warn("Error on binding={} eventId={} customerId={}",
                                bindingName, event.eventId(), event.customer().customerId());
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        null,
                        err -> log.error("Failed to publish to {}: {}", bindingName, err.getMessage(), err));
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

package com.ezra.customerbackend.service;

import com.ezra.customerbackend.notification.CustomerNotificationEvent;
import com.ezra.customerbackend.notification.CustomerNotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerNotificationService implements CustomerNotificationPublisher {

    private static final String NOTIFICATION_REQUEST_BINDING = "notification-request-out-0";
    private static final String CUSTOMER_NOTIFICATION_BINDING = "customer-notifications-out-0";

    private final StreamBridge streamBridge;

    @Override
    public void publish(CustomerNotificationEvent event) {
        sendAsync(NOTIFICATION_REQUEST_BINDING, event);
    }

    private void sendAsync(String bindingName, CustomerNotificationEvent event) {
        Mono.fromRunnable(() -> {
                    boolean sent = streamBridge.send(bindingName, event);
                    if (!sent) {
                        log.warn("StreamBridge returned false for binding={} customerId={}", bindingName, event.customerId());
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        null,
                        err -> log.error("Failed to publish to {}: {}", bindingName, err.getMessage(), err));
    }
}

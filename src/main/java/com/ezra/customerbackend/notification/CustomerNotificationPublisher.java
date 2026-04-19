package com.ezra.customerbackend.notification;

public interface CustomerNotificationPublisher {

    void publish(CustomerNotificationEvent event);

}

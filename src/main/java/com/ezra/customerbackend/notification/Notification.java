package com.ezra.customerbackend.notification;

import java.util.List;

public record Notification(String message, List<String> channels) {
}
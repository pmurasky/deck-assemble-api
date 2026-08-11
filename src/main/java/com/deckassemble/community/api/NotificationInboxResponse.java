package com.deckassemble.community.api;

import java.util.List;

record NotificationInboxResponse(List<NotificationResponse> notifications, long unreadCount) {}

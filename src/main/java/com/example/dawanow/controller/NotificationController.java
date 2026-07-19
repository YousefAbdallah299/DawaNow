package com.example.dawanow.controller;

import com.example.dawanow.dtos.response.NotificationResponse;
import com.example.dawanow.entity.notification.NotificationRecipient;
import com.example.dawanow.service.CurrentPharmacistProvider;
import com.example.dawanow.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Notification inbox for the currently authenticated pharmacist —
 * covers PHARMACY_INVITATION, ORDER_IN_AREA, and OFFER_ACCEPTED alike,
 * since all three land in the same notification_recipient table.
 */
@RestController
@RequestMapping("/api/v1/pharmacists/me/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentPharmacistProvider currentPharmacist;

    /**
     * Paginated notification list, optionally filtered by delivery/read status.
     * Example: GET /api/v1/pharmacists/me/notifications?status=SENT&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> list(
            @RequestParam(required = false) NotificationRecipient.Status status,
            Pageable pageable) {
        Long pharmacistId = currentPharmacist.get().getId();
        return ResponseEntity.ok(notificationService.list(pharmacistId, status, pageable));
    }

    /**
     * Unread count — typically used to render a badge on the bell icon.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Long> unreadCount() {
        Long pharmacistId = currentPharmacist.get().getId();
        return ResponseEntity.ok(notificationService.countUnread(pharmacistId));
    }

    @PatchMapping("/{recipientId}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long recipientId) {
        Long pharmacistId = currentPharmacist.get().getId();
        notificationService.markRead(pharmacistId, recipientId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        Long pharmacistId = currentPharmacist.get().getId();
        notificationService.markAllRead(pharmacistId);
        return ResponseEntity.noContent().build();
    }
}
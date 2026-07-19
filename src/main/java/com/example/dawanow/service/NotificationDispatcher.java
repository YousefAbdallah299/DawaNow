package com.example.dawanow.service;

import com.example.dawanow.config.notification.FcmClient;
import com.example.dawanow.entity.notification.DeviceToken;
import com.example.dawanow.entity.notification.Notification;
import com.example.dawanow.entity.notification.NotificationRecipient;
import com.example.dawanow.repo.DeviceTokenRepository;
import com.example.dawanow.repo.NotificationRecipientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationRecipientRepository recipientRepository;
    private final FcmClient fcmClient;

    @Async
    public void dispatch(Notification notification, List<NotificationRecipient> recipients) {
        List<Long> pharmacistIds = recipients.stream().map(NotificationRecipient::getPharmacistId).toList();
        List<DeviceToken> tokens = deviceTokenRepository.findByPharmacistIdInAndActiveTrue(pharmacistIds);

        if (tokens.isEmpty()) {
            return;
        }

        Map<String, String> dataPayload = notification.getDataPayload() == null
                ? Map.of()
                : notification.getDataPayload().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));

        FcmClient.DispatchResult result = fcmClient.send(
                tokens, notification.getTitle(), notification.getBody(), dataPayload);

        applyResult(notification, recipients, tokens, result);
    }

    @Transactional
    protected void applyResult(Notification notification, List<NotificationRecipient> recipients,
                               List<DeviceToken> tokens, FcmClient.DispatchResult result) {
        if (!result.deadTokens.isEmpty()) {
            deviceTokenRepository.deactivateAll(result.deadTokens.stream().map(DeviceToken::getId).toList());
        }

        Map<Long, NotificationRecipient> byPharmacistId = recipients.stream()
                .collect(Collectors.toMap(NotificationRecipient::getPharmacistId, r -> r));

        result.succeeded.forEach(token -> {
            NotificationRecipient recipient = byPharmacistId.get(token.getPharmacistId());
            if (recipient != null && recipient.getStatus() == NotificationRecipient.Status.PENDING) {
                recipient.markSent();
            }
        });

        recipients.stream()
                .filter(r -> r.getStatus() == NotificationRecipient.Status.PENDING)
                .forEach(NotificationRecipient::markFailed);

        recipientRepository.saveAll(recipients);
    }
}

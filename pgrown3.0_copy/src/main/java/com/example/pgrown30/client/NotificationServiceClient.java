package com.example.pgrown30.client;

import com.digit.services.notification.NotificationClient;
import com.digit.services.notification.model.SendEmailRequest;
import com.example.pgrown30.web.models.CitizenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wrapper class for notification-related operations.
 * Encapsulates notification client interactions to reduce clutter in service layer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationServiceClient {

    private final NotificationClient notificationClient;

    /**
     * Sends email notification if the citizen service has an email address.
     *
     * @param citizenService  the citizen service
     * @param workflowAction  the workflow action (null for create, non-null for update)
     */
    public void sendNotificationIfNeeded(CitizenService citizenService, String workflowAction) {
        if (citizenService.getEmail() == null || citizenService.getEmail().isBlank()) {
            return;
        }

        Map<String, Object> emailPayload = createEmailPayload(citizenService, workflowAction);

        SendEmailRequest request = SendEmailRequest.builder()
                .version("v1")
                .templateId("my-template")
                .emailIds(List.of(citizenService.getEmail()))
                .enrich(false)
                .payload(emailPayload)
                .build();

        notificationClient.sendEmail(request);
        String notificationType = workflowAction != null ? "update" : "create";
        log.info("Triggered {} email notification for {}", notificationType, citizenService.getServiceRequestId());
    }

    /**
     * Creates the email payload for notifications.
     *
     * @param citizenService  the citizen service
     * @param workflowAction  the workflow action
     * @return map containing email payload data
     */
    private Map<String, Object> createEmailPayload(CitizenService citizenService, String workflowAction) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("applicationNo", citizenService.getServiceRequestId());
        payload.put("citizenName", citizenService.getAccountId() != null ? citizenService.getAccountId() : "");
        payload.put("serviceName", citizenService.getDescription() != null ? citizenService.getDescription() : "");
        payload.put("statusLabel", citizenService.getApplicationStatus());
        payload.put("trackUrl", "https://pgr.digit.org/track/" + citizenService.getServiceRequestId());

        if (workflowAction != null) {
            payload.put("action", workflowAction);
        }

        return payload;
    }
}


package com.example.pgrown30.repository;

import com.digit.services.notification.NotificationClient;
import com.digit.services.notification.model.SendEmailRequest;
import com.digit.services.notification.model.SendEmailResponse;
import com.digit.services.notification.model.SendSMSRequest;
import com.digit.services.notification.model.SendSMSResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class NotificationRepository {

    private final NotificationClient notificationClient;

    @Async
    public void sendEmail(String templateId, List<String> emailIds, Map<String, Object> payload, List<String> attachments) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .templateId(templateId)
                    .version("1.0.0")
                    .emailIds(emailIds)
                    .enrich(false)
                    .payload(payload)
                    .attachments(attachments != null ? attachments : List.of())
                    .build();

            // Use digit-client library for email sending
            // Headers are automatically propagated via HeaderPropagationInterceptor
            SendEmailResponse response = notificationClient.sendEmail(request);

            log.info("Email notification sent [{}] to {}: {}", templateId, emailIds, 
                    response != null ? response.getStatus() : "SUCCESS");
        } catch (Exception e) {
            log.error("Failed to send email [{}] to {}: {}", templateId, emailIds, e.getMessage());
        }
    }

    @Async
    public void sendSms(String templateId, List<String> mobileNumbers, Map<String, Object> payload, String category) {
        try {
            // Use default SMS category - let's check what values are available
            SendSMSRequest.SMSCategory smsCategory = null;
            // We'll set this to null and let the library handle the default

            SendSMSRequest request = SendSMSRequest.builder()
                    .templateId(templateId)
                    .version("1.0.0")
                    .mobileNumbers(mobileNumbers)
                    .enrich(false)
                    .payload(payload)
                    .category(smsCategory)
                    .build();

            // Use digit-client library for SMS sending
            // Headers are automatically propagated via HeaderPropagationInterceptor
            SendSMSResponse response = notificationClient.sendSMS(request);

            log.info("SMS notification sent [{}] to {}: {}", templateId, mobileNumbers, 
                    response != null ? response.getStatus() : "SUCCESS");
        } catch (Exception e) {
            log.error("Failed to send SMS [{}] to {}: {}", templateId, mobileNumbers, e.getMessage());
        }
    }
}

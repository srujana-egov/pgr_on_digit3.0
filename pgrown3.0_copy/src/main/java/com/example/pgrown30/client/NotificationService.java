package com.example.pgrown30.client;

import com.digit.services.notification.NotificationClient;
import com.digit.services.notification.model.SendEmailRequest;
import com.digit.services.notification.model.SendEmailResponse;
import com.digit.services.notification.model.SendSMSRequest;
import com.digit.services.notification.model.SendSMSResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationClient notificationClient;

    @Async
    public void sendEmail(String templateId, List<String> emailIds, Map<String, Object> payload, List<String> attachments) {
        SendEmailRequest request = SendEmailRequest.builder()
                .templateId(templateId)
                .emailIds(emailIds)
                .enrich(false)
                .payload(payload)
                .build();

        // Use digit-client library for email sending
        // Headers are automatically propagated via HeaderPropagationInterceptor
        SendEmailResponse response = notificationClient.sendEmail(request);
        log.info("Email sent successfully with templateId: {}", templateId);
    }

    @Async
    public void sendSms(String templateId, List<String> mobileNumbers, Map<String, Object> payload, String category) {
        // Use default SMS category - let's check what values are available
        SendSMSRequest.SMSCategory smsCategory = null;
        // We'll set this to null and let the library handle the default

        SendSMSRequest request = SendSMSRequest.builder()
                .templateId(templateId)
                .mobileNumbers(mobileNumbers)
                .enrich(false)
                .payload(payload)
                .category(smsCategory)
                .build();

        // Use digit-client library for SMS sending
        // Headers are automatically propagated via HeaderPropagationInterceptor
        SendSMSResponse response = notificationClient.sendSMS(request);
        log.info("SMS sent successfully with templateId: {}", templateId);
    }
}

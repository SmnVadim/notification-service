package ru.aston.notification.service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.aston.notification.service.dto.EmailRequestDto;
import ru.aston.notification.service.dto.EmailResponseDto;
import ru.aston.notification.service.dto.UserEvent;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    public final EmailService emailService;

    public void handleEvent(UserEvent event) {
        log.info("Handling user event: operation={}, email={}", event.getOperation(), event.getEmail());

        validateEvent(event);
        emailService.sendEmail(event.getEmail(), event.getOperation());

        log.info("User event handled successfully for {}", event.getEmail());
    }

    public EmailResponseDto sendNotification(EmailRequestDto request) {
        log.info("Handling notification request: operation={}, email={}", request.getOperation(), request.getEmail());

        emailService.sendEmail(request.getEmail(), request.getOperation());

        return EmailResponseDto.builder()
                .email(request.getEmail())
                .operation(request.getOperation())
                .status("SENT")
                .sentAt(LocalDateTime.now())
                .build();
    }

    private void validateEvent(UserEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Even must not be null");
        }
        if (event.getEmail() == null || event.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        if (event.getOperation() == null) {
            throw new IllegalArgumentException("Operation must not be null");
        }
    }
}

package ru.aston.notification.service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aston.notification.service.dto.EmailRequestDto;
import ru.aston.notification.service.dto.EmailResponseDto;
import ru.aston.notification.service.exception.EmailSendingException;
import ru.aston.notification.service.service.EmailService;
import ru.aston.notification.service.service.NotificationService;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<EmailResponseDto> sendEmail(@Valid @RequestBody EmailRequestDto request) {

        log.info("REST request to send email: operation={}, email={}",
                request.getOperation(), request.getEmail());

        try {
            EmailResponseDto response = notificationService.sendNotification(request);
            return ResponseEntity.ok(response);
        } catch (EmailSendingException | IllegalArgumentException e) {
            log.warn("Failed to send notification to {}: {}",
                    request.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}

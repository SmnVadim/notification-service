package ru.aston.notification.service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import ru.aston.notification.service.exception.EmailSendingException;
import ru.aston.notification.service.dto.UserEvent.Operation;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.subject.created}")
    private String subjectCreated;

    @Value("${app.mail.subject.deleted}")
    private String subjectDeleted;

    private static final String BODY_CREATED = "Здравствуйте! Ваш аккаунт на сайте был успешно создан!";
    private static final String BODY_DELETED = "Здравствуйте! Ваш аккаунт был удалён!";

    public void sendEmail(String email, Operation operation) {
        log.info("Sender email to {} for operation {}", email, operation);

        String subject = resolveSubject(operation);
        String body = resolveBody(operation);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(email);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Email sent successfully to {} [{}]", email, operation);
        } catch (Exception e) {
            log.error("Failed to send email to {} [{}]", email, operation, e);
            throw new EmailSendingException("Failed to send email to " + email, e);
        }
    }

    private String resolveSubject(Operation operation) {
        return switch (operation) {
            case CREATE -> subjectCreated;
            case DELETE -> subjectDeleted;
        };
    }

    private String resolveBody(Operation operation) {
        return switch (operation) {
            case CREATE -> subjectCreated;
            case DELETE -> subjectDeleted;
        };
    }
}

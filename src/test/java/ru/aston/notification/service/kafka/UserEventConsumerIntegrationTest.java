package ru.aston.notification.service.kafka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.aston.notification.service.dto.UserEvent.Operation;
import ru.aston.notification.service.support.AbstractIntegrationTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserEventConsumerIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Событие CREATE из Kafka приводит к письму о создании аккаунта")
    void createEventSendsEmail() {
        String recipient = "create-" + UUID.randomUUID() + "@mail.test";

        sendUserEvent(Operation.CREATE, recipient);

        MailMessage message = awaitMessageTo(recipient);
        assertThat(message.to()).isEqualTo(recipient);
        assertThat(message.from()).isEqualTo("noreply@user-service.local");
        assertThat(message.subject()).isEqualTo("Your account has been successfully created");
        assertThat(message.body()).contains("Здравствуйте! Ваш аккаунт на сайте был успешно создан!");
    }

    @Test
    @DisplayName("Событие DELETE из Kafka приводит к письму об удалении аккаунта")
    void deleteEventSendsEmail() {
        String recipient = "delete-" + UUID.randomUUID() + "@mail.test";

        sendUserEvent(Operation.DELETE, recipient);

        MailMessage message = awaitMessageTo(recipient);
        assertThat(message.to()).isEqualTo(recipient);
        assertThat(message.from()).isEqualTo("noreply@user-service.local");
        assertThat(message.subject()).isEqualTo("Your account has been deleted");
        assertThat(message.body()).contains("Здравствуйте! Ваш аккаунт был удалён!");
    }
}

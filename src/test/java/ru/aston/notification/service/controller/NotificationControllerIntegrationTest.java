package ru.aston.notification.service.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.aston.notification.service.support.AbstractIntegrationTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("POST /api/notifications/send с операцией CREATE отправляет письмо и отвечает 200")
    void sendCreateEmailReturns200AndSendsEmail() throws Exception {
        String recipient = "rest-create-" + UUID.randomUUID() + "@mail.test";

        mockMvc.perform(post("/api/notifications/send")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"%s","operation":"CREATE"}""".formatted(recipient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(recipient))
                .andExpect(jsonPath("$.operation").value("CREATE"))
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.sentAt").exists());

        MailMessage message = awaitMessageTo(recipient);
        assertThat(message.from()).isEqualTo("noreply@user-service.local");
        assertThat(message.subject()).isEqualTo("Your account has been successfully created");
        assertThat(message.body()).contains("Здравствуйте! Ваш аккаунт на сайте был успешно создан!");
    }

    @Test
    @DisplayName("POST /api/notifications/send с операцией DELETE отправляет письмо и отвечает 200")
    void sendDeleteEmailReturns200AndSendsEmail() throws Exception {
        String recipient = "rest-delete-" + UUID.randomUUID() + "@mail.test";

        mockMvc.perform(post("/api/notifications/send")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"%s","operation":"DELETE"}""".formatted(recipient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(recipient))
                .andExpect(jsonPath("$.operation").value("DELETE"))
                .andExpect(jsonPath("$.status").value("SENT"));

        MailMessage message = awaitMessageTo(recipient);
        assertThat(message.subject()).isEqualTo("Your account has been deleted");
        assertThat(message.body()).contains("Здравствуйте! Ваш аккаунт был удалён!");
    }

    @Test
    @DisplayName("POST /api/notifications/send без почты отвечает 400 и ничего не отправляет")
    void sendWithoutEmailReturns400() throws Exception {
        mockMvc.perform(post("/api/notifications/send")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"","operation":"CREATE"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    @DisplayName("POST /api/notifications/send с некорректной почтой отвечает 400 и ничего не отправляет")
    void sendWithMalformedEmailReturns400() throws Exception {
        mockMvc.perform(post("/api/notifications/send")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","operation":"CREATE"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    @DisplayName("POST /api/notifications/send без операции отвечает 400")
    void sendWithoutOperationReturns400() throws Exception {
        mockMvc.perform(post("/api/notifications/send")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}""".formatted("rest-no-operation-" + UUID.randomUUID() + "@mail.test")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.operation").exists());
    }
}

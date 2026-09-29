package ru.aston.notification.service.support;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;
import ru.aston.notification.service.dto.UserEvent;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @DynamicPropertySource
    static void dockerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", TestEnvironment.KAFKA::getBootstrapServers);
        registry.add("spring.kafka.consumer.group-id", () -> TestEnvironment.CONSUMER_GROUP);
        registry.add("app.kafka.topic.user-events", () -> TestEnvironment.TOPIC);

        registry.add("spring.mail.host", TestEnvironment.MAILHOG::getHost);
        registry.add("spring.mail.port",
                () -> TestEnvironment.MAILHOG.getMappedPort(TestEnvironment.MAILHOG_SMTP_PORT));
        registry.add("spring.mail.username", () -> "");
        registry.add("spring.mail.password", () -> "");
        registry.add("spring.mail.properties.mail.smtp.auth", () -> "false");
        registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");
    }

    @BeforeEach
    protected void resetMailHog() {
        mailHog().delete().uri("/api/v1/messages").retrieve().toBodilessEntity();
    }

    protected void sendUserEvent(UserEvent.Operation operation, String email) {
        UserEvent event = UserEvent.builder()
                .operation(operation)
                .email(email)
                .timestamp(LocalDateTime.now())
                .build();

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, TestEnvironment.KAFKA.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>(TestEnvironment.TOPIC, email, objectMapper.writeValueAsString(event)))
                    .get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось отправить событие " + operation + " в Kafka", e);
        }
    }

    protected MailMessage awaitMessageTo(String recipient) {
        return Awaitility.await("Ожидание письма для " + recipient)
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(500))
                .until(() -> findMessageTo(recipient), Objects::nonNull);
    }

    protected MailMessage findMessageTo(String recipient) {
        String response = mailHog()
                .get()
                .uri("/api/v2/messages")
                .retrieve()
                .body(String.class);

        JsonNode items = objectMapper.readTree(response).path("items");
        if (!items.isArray()) {
            return null;
        }

        try {
            for (JsonNode item : items) {
                MimeMessage mimeMessage = parse(item.path("Raw").path("Data").asString(""));
                if (mimeMessage == null) {
                    continue;
                }
                for (Address address : mimeMessage.getAllRecipients()) {
                    if (recipient.equalsIgnoreCase(address.toString())) {
                        return new MailMessage(
                                String.valueOf(mimeMessage.getFrom()[0]),
                                address.toString(),
                                mimeMessage.getSubject(),
                                readBody(mimeMessage));
                    }
                }
            }
        } catch (MessagingException e) {
            throw new IllegalStateException("Не удалось разобрать письмо из MailHog", e);
        }
        return null;
    }

    private MimeMessage parse(String rawData) {
        if (rawData.isBlank()) {
            return null;
        }
        try {
            return new MimeMessage(Session.getInstance(new Properties()),
                    new ByteArrayInputStream(rawData.getBytes(StandardCharsets.UTF_8)));
        } catch (MessagingException e) {
            throw new IllegalStateException("Не удалось разобрать письмо из MailHog", e);
        }
    }

    private String readBody(MimeMessage mimeMessage) {
        try {
            Object content = mimeMessage.getContent();
            return content instanceof String text ? text : String.valueOf(content);
        } catch (MessagingException | IOException e) {
            throw new IllegalStateException("Не удалось прочитать тело письма", e);
        }
    }

    protected String mailHogUrl() {
        return "http://" + TestEnvironment.MAILHOG.getHost()
                + ":" + TestEnvironment.MAILHOG.getMappedPort(TestEnvironment.MAILHOG_API_PORT);
    }

    private RestClient mailHog() {
        return RestClient.create(mailHogUrl());
    }

    public record MailMessage(String from, String to, String subject, String body) {
    }
}

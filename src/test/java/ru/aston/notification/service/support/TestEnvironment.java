package ru.aston.notification.service.support;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class TestEnvironment {

    public static final int MAILHOG_SMTP_PORT = 1025;
    public static final int MAILHOG_API_PORT = 8025;

    public static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.7.0");

    public static final GenericContainer<?> MAILHOG =
            new GenericContainer<>(DockerImageName.parse("mailhog/mailhog:latest"))
                    .withExposedPorts(MAILHOG_SMTP_PORT, MAILHOG_API_PORT);

    public static final String TOPIC = "user-events-it-" + UUID.randomUUID();
    public static final String CONSUMER_GROUP = "notification-it-" + UUID.randomUUID();

    static {
        KAFKA.start();
        MAILHOG.start();
        createTopic();
    }

    private TestEnvironment() {
    }

    private static void createTopic() {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());

        try (Admin admin = Admin.create(props)) {
            admin.createTopics(List.of(new NewTopic(TOPIC, 1, (short) 1)))
                    .all()
                    .get(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Не удалось создать топик " + TOPIC, e);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось создать топик " + TOPIC, e);
        }
    }
}

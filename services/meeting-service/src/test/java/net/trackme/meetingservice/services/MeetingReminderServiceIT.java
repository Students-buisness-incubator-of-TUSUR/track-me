package net.trackme.meetingservice.services;

import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "app.app-url=http://localhost:8082")
@Testcontainers
class MeetingReminderServiceIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        // схема meeting_service не существует в чистом testcontainer — используем public
        registry.add("MEETING_DB_SCHEMA", () -> "public");
    }

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired
    MeetingReminderService meetingReminderService;

    @Autowired
    MeetingRepository meetingRepository;

    @BeforeEach
    void setUp() {
        meetingRepository.deleteAll();
    }

    @Test
    void sendReminders_scheduledMeetingIn3Days_producesReminderEvent() {
        ZoneId tomsk = ZoneId.of("Asia/Tomsk");
        OffsetDateTime startDate = LocalDate.now(tomsk).plusDays(3)
                .atTime(12, 0).atZone(tomsk).toOffsetDateTime();

        meetingRepository.save(Meeting.builder()
                .status(MeetingStatus.SCHEDULED)
                .startDate(startDate)
                .trackerUsername("tracker1")
                .teamName("TeamAlpha")
                .teamCardId(UUID.randomUUID())
                .build());

        // subscribe BEFORE producing so the consumer doesn't miss the message
        try (KafkaConsumer<String, String> consumer = createConsumer("it-group-produce-1", "earliest")) {
            consumer.subscribe(List.of("meeting-reminder"));
            consumer.poll(Duration.ofSeconds(2)); // force partition assignment

            meetingReminderService.sendReminders();

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
            assertThat(records.count()).isEqualTo(1);
            String payload = records.iterator().next().value();
            assertThat(payload).contains("tracker1").contains("TeamAlpha");
        }
    }

    @Test
    void sendReminders_completedMeeting_noEventProduced() {
        ZoneId tomsk = ZoneId.of("Asia/Tomsk");
        OffsetDateTime startDate = LocalDate.now(tomsk).plusDays(3)
                .atTime(12, 0).atZone(tomsk).toOffsetDateTime();

        meetingRepository.save(Meeting.builder()
                .status(MeetingStatus.COMPLETED)
                .startDate(startDate)
                .trackerUsername("tracker1")
                .teamName("TeamAlpha")
                .teamCardId(UUID.randomUUID())
                .build());

        // use "latest" so we don't read messages produced by other tests
        try (KafkaConsumer<String, String> consumer = createConsumer("it-group-noproduce-2", "latest")) {
            consumer.subscribe(List.of("meeting-reminder"));
            consumer.poll(Duration.ofSeconds(2)); // force partition assignment at latest offset

            meetingReminderService.sendReminders();

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(3));
            assertThat(records.isEmpty()).isTrue();
        }
    }

    @Test
    void sendReminders_meetingWithNoTracker_noEventProduced() {
        ZoneId tomsk = ZoneId.of("Asia/Tomsk");
        OffsetDateTime startDate = LocalDate.now(tomsk).plusDays(3)
                .atTime(12, 0).atZone(tomsk).toOffsetDateTime();

        meetingRepository.save(Meeting.builder()
                .status(MeetingStatus.SCHEDULED)
                .startDate(startDate)
                .trackerUsername(null)
                .teamName("TeamBeta")
                .teamCardId(UUID.randomUUID())
                .build());

        // use "latest" so we don't read messages produced by other tests
        try (KafkaConsumer<String, String> consumer = createConsumer("it-group-noproduce-3", "latest")) {
            consumer.subscribe(List.of("meeting-reminder"));
            consumer.poll(Duration.ofSeconds(2)); // force partition assignment at latest offset

            meetingReminderService.sendReminders();

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(3));
            assertThat(records.isEmpty()).isTrue();
        }
    }

    private KafkaConsumer<String, String> createConsumer(String groupId, String offsetReset) {
        return new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, groupId,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetReset,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        ));
    }
}

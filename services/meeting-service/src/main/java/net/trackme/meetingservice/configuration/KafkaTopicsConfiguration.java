package net.trackme.meetingservice.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static org.apache.kafka.common.config.TopicConfig.CLEANUP_POLICY_CONFIG;
import static org.apache.kafka.common.config.TopicConfig.CLEANUP_POLICY_DELETE;
import static org.apache.kafka.common.config.TopicConfig.RETENTION_MS_CONFIG;

@Configuration
public class KafkaTopicsConfiguration {
    public static final String MEETING_CREATED_TOPIC = "meeting-created";
    public static final String MEETING_UPDATED_TOPIC = "meeting-updated";
    public static final String MEETINGS_SUMMARY_TOPIC = "meeting-summary";

    @Bean
    NewTopic meetingCreatedTopic() {
        return new NewTopic(MEETING_CREATED_TOPIC, 3, (short) 1)
                .configs(Map.of(
                        // Пример настроек: хранить 7 дней
                        RETENTION_MS_CONFIG, String.valueOf(7L * 24 * 60 * 60 * 1000),
                        CLEANUP_POLICY_CONFIG, CLEANUP_POLICY_DELETE
                ));
    }

    @Bean
    NewTopic meetingUpdatedTopic() {
        return new NewTopic(MEETING_UPDATED_TOPIC, 3, (short) 1)
                .configs(Map.of(
                        // Пример настроек: хранить 7 дней
                        RETENTION_MS_CONFIG, String.valueOf(7L * 24 * 60 * 60 * 1000),
                        CLEANUP_POLICY_CONFIG, CLEANUP_POLICY_DELETE
                ));
    }

    @Bean
    NewTopic meetingsSummaryTopic() {
        return new NewTopic(MEETINGS_SUMMARY_TOPIC, 3, (short) 1)
                .configs(Map.of(
                        // Пример настроек: хранить 7 дней
                        RETENTION_MS_CONFIG, String.valueOf(7L * 24 * 60 * 60 * 1000),
                        CLEANUP_POLICY_CONFIG, CLEANUP_POLICY_DELETE
                ));
    }
}

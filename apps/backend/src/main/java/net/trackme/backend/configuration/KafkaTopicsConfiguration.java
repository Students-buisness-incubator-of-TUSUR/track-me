package net.trackme.backend.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static org.apache.kafka.common.config.TopicConfig.CLEANUP_POLICY_CONFIG;
import static org.apache.kafka.common.config.TopicConfig.CLEANUP_POLICY_DELETE;
import static org.apache.kafka.common.config.TopicConfig.RETENTION_MS_CONFIG;

/**
 * Объявление Kafka-топиков.
 */
@Configuration
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTopicsConfiguration {
    public static final String MEETING_NOT_HAPPENED_TOPIC = "meeting-not-happened";
    public static final String TEAM_CARD_SUMMARY_TOPIC = "team-card-summary";
    public static final String TEAM_CARD_LOW_GRADE_SUMMARY_TOPIC = "team-card-low-grade-summary";
    public static final String TEAM_CARD_DELETED_TOPIC = "team-card-deleted";

    @Bean
    NewTopic meetingNotHappenedTopic() {
        return new NewTopic(MEETING_NOT_HAPPENED_TOPIC, 3, (short) 1)
                .configs(Map.of(
                        // Пример настроек: хранить 7 дней
                        RETENTION_MS_CONFIG, String.valueOf(7L * 24 * 60 * 60 * 1000),
                        CLEANUP_POLICY_CONFIG, CLEANUP_POLICY_DELETE
                ));
    }

    @Bean
    NewTopic teamCardSummaryTopic() {
        return new NewTopic(TEAM_CARD_SUMMARY_TOPIC, 3, (short) 1)
                .configs(Map.of(
                        // Пример настроек: хранить 7 дней
                        RETENTION_MS_CONFIG, String.valueOf(7L * 24 * 60 * 60 * 1000),
                        CLEANUP_POLICY_CONFIG, CLEANUP_POLICY_DELETE
                ));
    }

    @Bean
    NewTopic teamCardLowGradeSummaryTopic() {
        return new NewTopic(TEAM_CARD_LOW_GRADE_SUMMARY_TOPIC, 3, (short) 1)
                .configs(Map.of(
                        // Пример настроек: хранить 7 дней
                        RETENTION_MS_CONFIG, String.valueOf(7L * 24 * 60 * 60 * 1000),
                        CLEANUP_POLICY_CONFIG, CLEANUP_POLICY_DELETE
                ));
    }

    @Bean
    NewTopic teamCardDeletedTopic() {
        return new NewTopic(TEAM_CARD_DELETED_TOPIC, 3, (short) 1)
                .configs(Map.of(
                        // Пример настроек: хранить 7 дней
                        RETENTION_MS_CONFIG, String.valueOf(7L * 24 * 60 * 60 * 1000),
                        CLEANUP_POLICY_CONFIG, CLEANUP_POLICY_DELETE
                ));
    }
}
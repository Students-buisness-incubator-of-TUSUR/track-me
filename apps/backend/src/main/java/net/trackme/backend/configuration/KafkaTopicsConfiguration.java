package net.trackme.backend.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class KafkaTopicsConfiguration {
    public static final String MEETING_NOT_HAPPENED_TOPIC = "meeting-not-happened";
    public static final String TEAM_CARD_SUMMARY_TOPIC = "team-card-summary";
    public static final String TEAM_CARD_LOW_GRADE_SUMMARY_TOPIC = "team-card-low-grade-summary";

    @Bean
    NewTopic meetingNotHappenedTopic() {
        return new NewTopic(MEETING_NOT_HAPPENED_TOPIC, 3, (short) 1)
                .configs(Map.of(
                        // Пример настроек: хранить 7 дней
                        "retention.ms", String.valueOf(7L * 24 * 60 * 60 * 1000),
                        "cleanup.policy", "delete"
                ));
    }

    @Bean
    NewTopic teamCardSummaryTopic() {
        return new NewTopic(TEAM_CARD_SUMMARY_TOPIC, 3, (short) 1)
                .configs(Map.of(
                        // Пример настроек: хранить 7 дней
                        "retention.ms", String.valueOf(7L * 24 * 60 * 60 * 1000),
                        "cleanup.policy", "delete"
                ));
    }

    @Bean
    NewTopic teamCardLowGradeSummaryTopic() {
        return new NewTopic(TEAM_CARD_LOW_GRADE_SUMMARY_TOPIC, 3, (short) 1)
                .configs(Map.of(
                        // Пример настроек: хранить 7 дней
                        "retention.ms", String.valueOf(7L * 24 * 60 * 60 * 1000),
                        "cleanup.policy", "delete"
                ));
    }
}
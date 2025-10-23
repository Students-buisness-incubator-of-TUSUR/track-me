package net.trackme.backend.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class KafkaTopicsConfiguration {
    public static final String MEETING_NOT_HAPPENED_TOPIC = "meeting-not-happened";

    @Bean
    NewTopic teamGradeFellTopic() {
        return new NewTopic(MEETING_NOT_HAPPENED_TOPIC, 3, (short) 1)
                .configs(Map.of(
                        // Пример настроек: хранить 7 дней
                        "retention.ms", String.valueOf(7L * 24 * 60 * 60 * 1000),
                        "cleanup.policy", "delete"
                ));
    }
}

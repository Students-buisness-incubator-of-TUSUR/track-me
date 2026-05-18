package net.trackme.sso.config;

import net.trackme.sso.messaging.MeetingNotHappenedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.LinkedHashMap;
import java.util.List;

@EnableKafka
@Configuration
public class KafkaConsumerConfiguration {
    
    private static final String TRUSTED_PACKAGES = "net.trackme.services.trackme-sso.messaging";

    /**
     * Generic factory для создания типобезопасных Kafka consumer'ов
     */
    private <T> ConsumerFactory<String, T> createConsumerFactory(
            KafkaProperties kafkaProperties,
            Class<T> valueType) {
        var props = kafkaProperties.buildConsumerProperties(null);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, TRUSTED_PACKAGES);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, valueType.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(valueType, false)
        );
    }

    @Bean
    ConsumerFactory<String, MeetingNotHappenedEvent> meetingNotHappenedEventConsumerFactory(
            KafkaProperties kafkaProperties) {
        return createConsumerFactory(kafkaProperties, MeetingNotHappenedEvent.class);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, MeetingNotHappenedEvent> meetingNotHappenedListenerContainerFactory(
            ConsumerFactory<String, MeetingNotHappenedEvent> meetingNotHappenedEventConsumerFactory
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, MeetingNotHappenedEvent>();
        factory.setConsumerFactory(meetingNotHappenedEventConsumerFactory);
        return factory;
    }

    @Bean
    ConsumerFactory<String, List<LinkedHashMap<String, String>>> teamCardSummaryEventConsumerFactory(
            KafkaProperties kafkaProperties) {
        return createConsumerFactory(kafkaProperties, (Class) List.class);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, List<LinkedHashMap<String, String>>> teamCardSummaryListenerContainerFactory(
            ConsumerFactory<String, List<LinkedHashMap<String, String>>> teamCardSummaryEventConsumerFactory
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, List<LinkedHashMap<String, String>>>();
        factory.setConsumerFactory(teamCardSummaryEventConsumerFactory);
        return factory;
    }

    @Bean
    ConsumerFactory<String, List<LinkedHashMap<String, String>>> teamCardLowGradeSummaryEventConsumerFactory(
            KafkaProperties kafkaProperties) {
        return createConsumerFactory(kafkaProperties, (Class) List.class);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, List<LinkedHashMap<String, String>>> teamCardLowGradeSummaryListenerContainerFactory(
            ConsumerFactory<String, List<LinkedHashMap<String, String>>> teamCardLowGradeSummaryEventConsumerFactory
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, List<LinkedHashMap<String, String>>>();
        factory.setConsumerFactory(teamCardLowGradeSummaryEventConsumerFactory);
        return factory;
    }
}

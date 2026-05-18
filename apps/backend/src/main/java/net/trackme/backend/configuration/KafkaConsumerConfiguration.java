package net.trackme.backend.configuration;

import net.trackme.backend.messaging.MeetingCreatedEvent;
import net.trackme.backend.messaging.MeetingDeletedEvent;
import net.trackme.backend.messaging.MeetingUpdatedEvent;
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

/**
 * Конфигурация Kafka потребителей.
 */
@EnableKafka
@Configuration
public class KafkaConsumerConfiguration {

    @Bean
    ConsumerFactory<String, MeetingCreatedEvent> meetingCreatedEventConsumerFactory(
            KafkaProperties kafkaProperties) {
        var props = kafkaProperties.buildConsumerProperties(null);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "net.trackme.backend.messaging");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, MeetingCreatedEvent.class);
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(MeetingCreatedEvent.class, false));
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, MeetingCreatedEvent>
            meetingCreatedListenerContainerFactory(
                    ConsumerFactory<String, MeetingCreatedEvent>
                            meetingCreatedEventConsumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, MeetingCreatedEvent>();
        factory.setConsumerFactory(meetingCreatedEventConsumerFactory);
        return factory;
    }

    @Bean
    ConsumerFactory<String, MeetingUpdatedEvent> meetingUpdatedEventConsumerFactory(
            KafkaProperties kafkaProperties) {
        var props = kafkaProperties.buildConsumerProperties(null);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "net.trackme.backend.messaging");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, MeetingUpdatedEvent.class);
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(MeetingUpdatedEvent.class, false));
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, MeetingUpdatedEvent>
            meetingUpdatedListenerContainerFactory(
                    ConsumerFactory<String, MeetingUpdatedEvent>
                            meetingUpdatedEventConsumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, MeetingUpdatedEvent>();
        factory.setConsumerFactory(meetingUpdatedEventConsumerFactory);
        return factory;
    }

    @Bean
    ConsumerFactory<String, MeetingDeletedEvent> meetingDeletedEventConsumerFactory(
            KafkaProperties kafkaProperties) {
        var props = kafkaProperties.buildConsumerProperties(null);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "net.trackme.backend.messaging");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, MeetingDeletedEvent.class);
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(MeetingDeletedEvent.class, false));
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, MeetingDeletedEvent>
            meetingDeletedListenerContainerFactory(
                    ConsumerFactory<String, MeetingDeletedEvent>
                            meetingDeletedEventConsumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, MeetingDeletedEvent>();
        factory.setConsumerFactory(meetingDeletedEventConsumerFactory);
        return factory;
    }

    @Bean
    ConsumerFactory<String, List<LinkedHashMap<String, String>>>
            meetingSummaryEventConsumerFactory(KafkaProperties kafkaProperties) {
        var props = kafkaProperties.buildConsumerProperties(null);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, List.class);
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(List.class, false));
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, List<LinkedHashMap<String, String>>>
            meetingSummaryListenerContainerFactory(
                    ConsumerFactory<String, List<LinkedHashMap<String, String>>>
                            meetingSummaryEventConsumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory
                <String, List<LinkedHashMap<String, String>>>();
        factory.setConsumerFactory(meetingSummaryEventConsumerFactory);
        return factory;
    }
}

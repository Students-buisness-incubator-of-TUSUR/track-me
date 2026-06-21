package net.trackme.telegramservice.configuration;


import net.trackme.telegramservice.messaging.MeetingNotHappenedEvent;
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

@EnableKafka
@Configuration
public class KafkaConsumerConfiguration {

    @Bean
    ConsumerFactory<String, MeetingNotHappenedEvent> meetingNotHappenedEventConsumerFactory(
            KafkaProperties kafkaProperties) {
        var props = kafkaProperties.buildConsumerProperties(null);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "net.trackme.services.telegram-service.messaging");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, MeetingNotHappenedEvent.class);
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(MeetingNotHappenedEvent.class, false)
        );
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, MeetingNotHappenedEvent> meetingNotHappenedListenerContainerFactory(
            ConsumerFactory<String, MeetingNotHappenedEvent> meetingNotHappenedEventConsumerFactory
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, MeetingNotHappenedEvent>();
        factory.setConsumerFactory(meetingNotHappenedEventConsumerFactory);
        return factory;
    }
}

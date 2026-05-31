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

/**
 * Конфигурация Kafka-потребителей для SSO-сервиса.
 * Настраивает фабрики потребителей и контейнеры для обработки
 * различных типов событий.
 */
@EnableKafka
@Configuration
public class KafkaConsumerConfiguration {

    /** Пакеты, которым разрешена десериализация. */
    private static final String TRUSTED_PACKAGES =
            "net.trackme.services.trackme-sso.messaging";

    /**
     * Создаёт типобезопасную фабрику Kafka-потребителей.
     *
     * @param <T> тип значения для десериализации из сообщений Kafka
     * @param kafkaProperties свойства Kafka из конфигурации приложения
     * @param valueType класс типа значения для десериализации
     * @return настроенная фабрика потребителей для указанного типа значений
     */
    private <T> ConsumerFactory<String, T> createConsumerFactory(
            KafkaProperties kafkaProperties,
            Class<T> valueType) {
        var props = kafkaProperties.buildConsumerProperties(null);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, TRUSTED_PACKAGES);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                valueType.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(valueType, false)
        );
    }

    /**
     * Создаёт контейнер фабрики слушателей Kafka для указанного типа.
     *
     * @param <T> тип значения, которое будет потребляться из Kafka
     * @param consumerFactory фабрика потребителей для создания слушателей
     * @return настроенный контейнер фабрики слушателей Kafka
     */
    private <T> ConcurrentKafkaListenerContainerFactory<String, T>
    createListenerContainerFactory(
            ConsumerFactory<String, T> consumerFactory) {
        var factory =
                new ConcurrentKafkaListenerContainerFactory<String, T>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    /**
     * Создаёт фабрику потребителей для событий о несостоявшихся встречах.
     *
     * @param kafkaProperties свойства Kafka из конфигурации приложения
     * @return фабрика потребителей для {@link MeetingNotHappenedEvent}
     */
    @Bean
    ConsumerFactory<String, MeetingNotHappenedEvent>
    meetingNotHappenedEventConsumerFactory(
            KafkaProperties kafkaProperties) {
        return createConsumerFactory(
                kafkaProperties, MeetingNotHappenedEvent.class);
    }

    /**
     * Создаёт контейнер фабрики слушателей для событий
     * о несостоявшихся встречах.
     *
     * @param meetingNotHappenedEventConsumerFactory фабрика потребителей
     * @return настроенный контейнер фабрики слушателей Kafka
     */
    @Bean
    ConcurrentKafkaListenerContainerFactory<String, MeetingNotHappenedEvent>
    meetingNotHappenedListenerContainerFactory(
            ConsumerFactory<String, MeetingNotHappenedEvent>
            meetingNotHappenedEventConsumerFactory) {
        return createListenerContainerFactory(
                meetingNotHappenedEventConsumerFactory);
    }

    /**
     * Создаёт фабрику потребителей для сводных событий
     * по карточкам команд.
     *
     * @param kafkaProperties свойства Kafka из конфигурации приложения
     * @return фабрика потребителей для списка карт команд
     */
    @Bean
    ConsumerFactory<String, List<LinkedHashMap<String, String>>>
    teamCardSummaryEventConsumerFactory(
            KafkaProperties kafkaProperties) {
        return createConsumerFactory(kafkaProperties,
                (Class) List.class);
    }

    /**
     * Создаёт контейнер фабрики слушателей для сводных событий
     * по карточкам команд.
     *
     * @param teamCardSummaryEventConsumerFactory фабрика потребителей
     * @return настроенный контейнер фабрики слушателей Kafka
     */
    @Bean
    ConcurrentKafkaListenerContainerFactory<
            String, List<LinkedHashMap<String, String>>>
    teamCardSummaryListenerContainerFactory(
            ConsumerFactory<String,
                    List<LinkedHashMap<String, String>>>
            teamCardSummaryEventConsumerFactory) {
        return createListenerContainerFactory(
                teamCardSummaryEventConsumerFactory);
    }

    /**
     * Создаёт фабрику потребителей для сводных событий
     * по карточкам команд с низкими оценками.
     *
     * @param kafkaProperties свойства Kafka из конфигурации приложения
     * @return фабрика потребителей для списка карт команд
     */
    @Bean
    ConsumerFactory<String, List<LinkedHashMap<String, String>>>
    teamCardLowGradeSummaryEventConsumerFactory(
            KafkaProperties kafkaProperties) {
        return createConsumerFactory(kafkaProperties,
                (Class) List.class);
    }

    /**
     * Создаёт контейнер фабрики слушателей для сводных событий
     * по карточкам команд с низкими оценками.
     *
     * @param teamCardLowGradeSummaryEventConsumerFactory фабрика потребителей
     * @return настроенный контейнер фабрики слушателей Kafka
     */
    @Bean
    ConcurrentKafkaListenerContainerFactory<
            String, List<LinkedHashMap<String, String>>>
    teamCardLowGradeSummaryListenerContainerFactory(
            ConsumerFactory<String,
                    List<LinkedHashMap<String, String>>>
            teamCardLowGradeSummaryEventConsumerFactory) {
        return createListenerContainerFactory(
                teamCardLowGradeSummaryEventConsumerFactory);
    }
}

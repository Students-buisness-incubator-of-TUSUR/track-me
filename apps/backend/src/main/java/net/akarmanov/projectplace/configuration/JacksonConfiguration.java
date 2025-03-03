package net.akarmanov.projectplace.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Настройки Jackson.
 */
@Configuration
public class JacksonConfiguration {

  /**
   * Правила сериализации и десериализации.
   *
   * @return Правила сериализации и десериализации.
   */
  @Bean
  public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
    return builder -> builder.featuresToDisable(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
        DeserializationFeature.ACCEPT_FLOAT_AS_INT,
        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
    );
  }
}

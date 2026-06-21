package net.trackme.backend.configuration;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Настройки доменной модели.
 */
@Configuration
@EntityScan(basePackages = "net.trackme.backend.domain")
@EnableJpaRepositories(basePackages = "net.trackme.backend.repos")
@EnableTransactionManagement
public class DomainConfig {
}

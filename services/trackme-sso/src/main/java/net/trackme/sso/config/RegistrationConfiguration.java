package net.trackme.sso.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.trackme.sso.components.RegistrationStore;
import net.trackme.sso.components.RegistrationTokenStore;
import net.trackme.sso.components.impl.RedisRegistrationStore;
import net.trackme.sso.components.impl.RedisRegistrationTokenStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RegistrationStoreProperties.class)
public class RegistrationConfiguration {
    private final RegistrationStoreProperties registrationStoreProperties;

    @Bean
    RegistrationStore redisRegistrationStore(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper) {
        return new RedisRegistrationStore(
                registrationStoreProperties.getCookieMaxAge(),
                stringRedisTemplate,
                stringRedisTemplate.opsForValue(),
                objectMapper);
    }

    @Bean
    RegistrationTokenStore redisRegistrationTokenStore(
            StringRedisTemplate stringRedisTemplate) {
        return new RedisRegistrationTokenStore(
                stringRedisTemplate,
                stringRedisTemplate.opsForValue()
        );
    }
}

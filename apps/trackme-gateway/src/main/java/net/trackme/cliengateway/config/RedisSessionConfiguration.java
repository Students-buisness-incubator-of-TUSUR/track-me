package net.trackme.cliengateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.ReactiveRedisSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.server.EnableRedisWebSession;
import org.springframework.session.web.server.session.SpringSessionWebSessionStore;
import org.springframework.web.server.session.WebSessionStore;

@Configuration
@EnableRedisWebSession
public class RedisSessionConfiguration {

    @Bean
    public WebSessionStore webSessionStore(ReactiveRedisSessionRepository sessionRepository) {
        return new SpringSessionWebSessionStore<>(sessionRepository);
    }
}

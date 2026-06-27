package net.trackme.meetingservice.services;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = "net.trackme.meetingservice",
        exclude = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class,
                RedisAutoConfiguration.class,
                CacheAutoConfiguration.class
        }
)
class MeetingReminderServiceITApp {}

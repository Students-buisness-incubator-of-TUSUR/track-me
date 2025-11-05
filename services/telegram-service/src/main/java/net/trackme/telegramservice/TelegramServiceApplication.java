package net.trackme.telegramservice;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@AllArgsConstructor
@SpringBootApplication
public class TelegramServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TelegramServiceApplication.class, args);
    }
}
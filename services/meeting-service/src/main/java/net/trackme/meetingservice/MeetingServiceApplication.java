package net.trackme.meetingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MeetingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MeetingServiceApplication.class, args);
    }
}

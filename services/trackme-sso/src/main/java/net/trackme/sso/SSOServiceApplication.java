package net.trackme.sso;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SSOServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(SSOServiceApplication.class, args);
  }
}
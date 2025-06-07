package net.trackme.backend;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication
public class ProjectPlaceApplication {

  public static void main(String[] args) {
    SpringApplication.run(ProjectPlaceApplication.class, args);
  }

  @Bean
  CommandLineRunner commandLineRunner() {
    return args -> log.info("Сервер запущен");
  }

}

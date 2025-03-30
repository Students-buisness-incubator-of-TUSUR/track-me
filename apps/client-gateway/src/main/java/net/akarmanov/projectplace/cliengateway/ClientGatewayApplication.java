package net.akarmanov.projectplace.cliengateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ClientGatewayApplication {
  public static void main(String[] args) {
    SpringApplication.run(ClientGatewayApplication.class, args);
  }

  @Bean
  RouteLocator routeLocator(RouteLocatorBuilder builder) {
    return builder.routes()
        .route("backend", r -> r.path("/**")
            .filters(GatewayFilterSpec::tokenRelay)
            .uri("http://localhost:8080"))
        .build();
  }
}
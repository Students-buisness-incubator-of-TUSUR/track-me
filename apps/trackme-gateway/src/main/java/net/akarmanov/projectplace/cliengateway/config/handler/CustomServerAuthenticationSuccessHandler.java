package net.akarmanov.projectplace.cliengateway.config.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomServerAuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {

  @Value("${spring.security.oauth2.client.location}")
  private String locationUrl;

  @Override
  public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange,
                                            Authentication authentication) {
    log.info("Authentication successful for user: {}", authentication.getName());
    var exchange = webFilterExchange.getExchange();
    var response = exchange.getResponse();
    response.getHeaders().setLocation(URI.create(locationUrl));
    response.setStatusCode(HttpStatus.FOUND);
    return response.setComplete();
  }
}

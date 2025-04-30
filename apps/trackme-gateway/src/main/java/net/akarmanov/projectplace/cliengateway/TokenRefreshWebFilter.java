package net.akarmanov.projectplace.cliengateway;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TokenRefreshWebFilter implements WebFilter {

  private final ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

  private final ServerOAuth2AuthorizedClientRepository authorizedClientRepository;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    return exchange.getPrincipal()
        .cast(OAuth2AuthenticationToken.class)
        .flatMap(oAuth2AuthenticationToken -> {
          var registrationId =
              oAuth2AuthenticationToken.getAuthorizedClientRegistrationId();
          var authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(registrationId)
              .principal(oAuth2AuthenticationToken)
              .build();
          return authorizedClientManager.authorize(authorizeRequest)
              .flatMap(authorizedClient -> {
                if (authorizedClient == null) {
                  return Mono.empty();
                }
                return authorizedClientRepository.saveAuthorizedClient(
                    authorizedClient, oAuth2AuthenticationToken, exchange);
              });
        })
        .then(chain.filter(exchange));
  }
}

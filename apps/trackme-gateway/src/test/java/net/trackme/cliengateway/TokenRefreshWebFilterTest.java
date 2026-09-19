package net.trackme.cliengateway;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.server.MockWebSession;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(
    classes = {TokenRefreshWebFilter.class},
    properties = {
        "spring.main.web-application-type=none",
        "spring.main.allow-bean-definition-overriding=true"
    })
class TokenRefreshWebFilterTest {

  @Autowired
  private TokenRefreshWebFilter tokenRefreshWebFilter;

  @MockitoBean
  private ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

  @MockitoBean
  private ServerOAuth2AuthorizedClientRepository authorizedClientRepository;

  @MockitoBean
  private ServerWebExchange mockExchange;

  @MockitoBean
  private WebFilterChain mockFilterChain;

  @Test
  void filter_whenPrincipalIsOAuth2AuthenticationTokenAndAuthorizedClientIsSaved() {
    // Arrange
    OAuth2AuthenticationToken authenticationToken = mock(OAuth2AuthenticationToken.class);
    when(authenticationToken.getAuthorizedClientRegistrationId()).thenReturn("test-registration-id");

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    when(exchange.getPrincipal()).thenReturn(Mono.just(authenticationToken));
    when(exchange.getSession()).thenReturn(Mono.just(new MockWebSession()));

    var mockAuthorizedClient = mock(OAuth2AuthorizedClient.class);
    when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(Mono.just(
        mockAuthorizedClient));

    when(authorizedClientRepository.saveAuthorizedClient(any(),
        any(),
        any())).thenReturn(Mono.empty());
    when(mockFilterChain.filter(exchange)).thenReturn(Mono.empty());

    // Act
    Mono<Void> result = tokenRefreshWebFilter.filter(exchange, mockFilterChain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    ArgumentCaptor<OAuth2AuthorizeRequest> authorizeRequestCaptor =
        ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);
    verify(authorizedClientManager).authorize(authorizeRequestCaptor.capture());
    verify(authorizedClientRepository).saveAuthorizedClient(mockAuthorizedClient,
        authenticationToken,
        exchange);
    verify(mockFilterChain).filter(exchange);
  }

  @Test
  void filter_whenPrincipalIsNotOAuth2AuthenticationToken() {
    // Arrange
    when(mockExchange.getPrincipal()).thenReturn(Mono.empty());
    when(mockFilterChain.filter(mockExchange)).thenReturn(Mono.empty());

    // Act
    Mono<Void> result = tokenRefreshWebFilter.filter(mockExchange, mockFilterChain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    verifyNoInteractions(authorizedClientManager);
    verifyNoInteractions(authorizedClientRepository);
    verify(mockFilterChain).filter(mockExchange);
  }

  @Test
  void filter_whenAuthorizedClientManagerReturnsNull() {
    // Arrange
    OAuth2AuthenticationToken authenticationToken = mock(OAuth2AuthenticationToken.class);
    when(authenticationToken.getAuthorizedClientRegistrationId()).thenReturn("test-registration-id");

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    when(exchange.getPrincipal()).thenReturn(Mono.just(authenticationToken));
    when(exchange.getSession()).thenReturn(Mono.just(new MockWebSession()));

    when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(Mono.empty());
    when(mockFilterChain.filter(exchange)).thenReturn(Mono.empty());

    // Act
    Mono<Void> result = tokenRefreshWebFilter.filter(exchange, mockFilterChain);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    verify(authorizedClientManager).authorize(any(OAuth2AuthorizeRequest.class));
    verifyNoInteractions(authorizedClientRepository);
    verify(mockFilterChain).filter(exchange);
  }
  @Test
  void concurrentRequestsShareRefreshAndSaveBothSessions() {
    var principal = mock(OAuth2AuthenticationToken.class);
    when(principal.getAuthorizedClientRegistrationId()).thenReturn("track-me-client");
    var session = new MockWebSession();
    var first = mock(ServerWebExchange.class);
    var second = mock(ServerWebExchange.class);
    when(first.getPrincipal()).thenReturn(Mono.just(principal));
    when(second.getPrincipal()).thenReturn(Mono.just(principal));
    when(first.getSession()).thenReturn(Mono.just(session));
    when(second.getSession()).thenReturn(Mono.just(session));
    var client = mock(OAuth2AuthorizedClient.class);
    var result = reactor.core.publisher.Sinks.<OAuth2AuthorizedClient>one();
    when(authorizedClientManager.authorize(any())).thenReturn(result.asMono());
    when(authorizedClientRepository.saveAuthorizedClient(any(), any(), any())).thenReturn(Mono.empty());
    when(mockFilterChain.filter(any())).thenReturn(Mono.empty());

    StepVerifier.create(Mono.when(tokenRefreshWebFilter.filter(first, mockFilterChain),
            tokenRefreshWebFilter.filter(second, mockFilterChain)))
        .then(() -> result.tryEmitValue(client))
        .verifyComplete();

    verify(authorizedClientManager, times(1)).authorize(any());
    verify(authorizedClientRepository).saveAuthorizedClient(client, principal, first);
    verify(authorizedClientRepository).saveAuthorizedClient(client, principal, second);
  }

}
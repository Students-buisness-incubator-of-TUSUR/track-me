package net.akarmanov.projectplace.cliengateway.config.handler;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamicRedirectServerAuthenticationSuccessHandlerTest {

  @Test
  void testOnAuthenticationSuccessWithXForwardedHeaders() {
    // Arrange
    String redirectUrl = "/dashboard";
    DynamicRedirectServerAuthenticationSuccessHandler successHandler =
        new DynamicRedirectServerAuthenticationSuccessHandler(redirectUrl);

    MockServerHttpRequest request = MockServerHttpRequest.get("https://example.com")
        .header("X-Forwarded-Scheme", "https")
        .header("X-Forwarded-Host", "example.com")
        .header("X-Forwarded-Port", "443")
        .build();

    var response = new MockServerHttpResponse();

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    when(exchange.getRequest()).thenReturn(request);
    when(exchange.getResponse()).thenReturn(response);
    when(exchange.getSession()).thenReturn(Mono.empty());

    WebFilterExchange webFilterExchange = mock(WebFilterExchange.class);
    when(webFilterExchange.getExchange()).thenReturn(exchange);

    Authentication authentication = mock(Authentication.class);

    // Calculate expected baseURL
    String expectedBaseUrl = UriComponentsBuilder.newInstance()
        .scheme("https")
        .host("example.com")
        .port(443)
        .path(redirectUrl)
        .build()
        .toUriString();

    // Act and Assert
    StepVerifier.create(successHandler.onAuthenticationSuccess(webFilterExchange, authentication))
        .verifyComplete();

    RedirectServerAuthenticationSuccessHandler redirectHandler =
        new RedirectServerAuthenticationSuccessHandler(expectedBaseUrl);
    StepVerifier.create(redirectHandler.onAuthenticationSuccess(webFilterExchange, authentication))
        .verifyComplete();
  }

  @Test
  void testOnAuthenticationSuccessWithoutXForwardedHeaders() {
    // Arrange
    String redirectUrl = "/home";
    DynamicRedirectServerAuthenticationSuccessHandler successHandler =
        new DynamicRedirectServerAuthenticationSuccessHandler(redirectUrl);

    MockServerHttpRequest request = MockServerHttpRequest.get("https://example.com").build();

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    when(exchange.getRequest()).thenReturn(request);
    when(exchange.getResponse()).thenReturn(new MockServerHttpResponse());
    when(exchange.getSession()).thenReturn(Mono.empty());

    WebFilterExchange webFilterExchange = mock(WebFilterExchange.class);
    when(webFilterExchange.getExchange()).thenReturn(exchange);

    Authentication authentication = mock(Authentication.class);

    // Calculate expected baseURL
    String expectedBaseUrl = UriComponentsBuilder.newInstance()
        .scheme("http")
        .host("localhost")
        .port(request.getURI().getPort())
        .path(request.getURI().getHost().equals("localhost") ? "/" : redirectUrl)
        .build()
        .toUriString();

    // Act and Assert
    StepVerifier.create(successHandler.onAuthenticationSuccess(webFilterExchange, authentication))
        .verifyComplete();

    RedirectServerAuthenticationSuccessHandler redirectHandler =
        new RedirectServerAuthenticationSuccessHandler(expectedBaseUrl);
    StepVerifier.create(redirectHandler.onAuthenticationSuccess(webFilterExchange, authentication))
        .verifyComplete();
  }

  @Test
  void testOnAuthenticationSuccessWithInvalidHost() {
    // Arrange
    String redirectUrl = "/default";
    DynamicRedirectServerAuthenticationSuccessHandler successHandler =
        new DynamicRedirectServerAuthenticationSuccessHandler(redirectUrl);

    MockServerHttpRequest request = MockServerHttpRequest.get("https://example.com")
        .build();

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    when(exchange.getRequest()).thenReturn(request);
    when(exchange.getResponse()).thenReturn(new MockServerHttpResponse());
    when(exchange.getSession()).thenReturn(Mono.empty());

    WebFilterExchange webFilterExchange = mock(WebFilterExchange.class);
    when(webFilterExchange.getExchange()).thenReturn(exchange);

    Authentication authentication = mock(Authentication.class);

    // Calculate expected baseURL
    String expectedBaseUrl = UriComponentsBuilder.newInstance()
        .scheme(request.getURI().getScheme())
        .host("localhost")
        .port(request.getURI().getPort())
        .path(redirectUrl)
        .build()
        .toUriString();

    // Act and Assert
    StepVerifier.create(successHandler.onAuthenticationSuccess(webFilterExchange, authentication))
        .verifyComplete();

    RedirectServerAuthenticationSuccessHandler redirectHandler =
        new RedirectServerAuthenticationSuccessHandler(expectedBaseUrl);
    StepVerifier.create(redirectHandler.onAuthenticationSuccess(webFilterExchange, authentication))
        .verifyComplete();
  }
}
package net.akarmanov.projectplace.cliengateway.config.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class DynamicRedirectServerAuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {

  private final String redirectUrl;

  @Override
  public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange,
                                            Authentication authentication) {
    var request = webFilterExchange.getExchange().getRequest();

    var scheme = request.getHeaders().getFirst("X-Forwarded-Scheme");
    var host = request.getHeaders().getFirst("X-Forwarded-Host");
    var port = request.getHeaders().getFirst("X-Forwarded-Port");

    if (host == null) {
      host = request.getURI().getHost();
      if (host == null) {
        host = "localhost";
      }
    }
    var baseUrl = UriComponentsBuilder.newInstance()
        .scheme(scheme == null ? request.getURI().getScheme() : scheme)
        .host(host)
        .port(port == null ? request.getURI().getPort() : Integer.parseInt(port))
        .path(request.getURI().getHost().equals(host) ? "/" : redirectUrl)
        .build()
        .toUriString();

    if (baseUrl.isBlank()) {
      baseUrl = redirectUrl;
    }

    return new RedirectServerAuthenticationSuccessHandler(baseUrl)
        .onAuthenticationSuccess(webFilterExchange, authentication);
  }
}

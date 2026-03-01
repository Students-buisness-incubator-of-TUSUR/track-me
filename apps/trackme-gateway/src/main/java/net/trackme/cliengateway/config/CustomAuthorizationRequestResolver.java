package net.trackme.cliengateway.config;

import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class CustomAuthorizationRequestResolver implements ServerOAuth2AuthorizationRequestResolver {

    static final String SESSION_KEY = "post_login_redirect_uri";

    private final DefaultServerOAuth2AuthorizationRequestResolver delegate;

    public CustomAuthorizationRequestResolver(ReactiveClientRegistrationRepository repository) {
        this.delegate = new DefaultServerOAuth2AuthorizationRequestResolver(repository);
    }

    @Override
    public Mono<OAuth2AuthorizationRequest> resolve(ServerWebExchange exchange) {
        return delegate.resolve(exchange)
                .flatMap(request -> saveRedirectUri(exchange, request));
    }

    @Override
    public Mono<OAuth2AuthorizationRequest> resolve(ServerWebExchange exchange, String clientRegistrationId) {
        return delegate.resolve(exchange, clientRegistrationId)
                .flatMap(request -> saveRedirectUri(exchange, request));
    }

    private Mono<OAuth2AuthorizationRequest> saveRedirectUri(ServerWebExchange exchange,
                                                              OAuth2AuthorizationRequest request) {
        String redirectUri = exchange.getRequest().getQueryParams().getFirst("redirect_uri");
        if (redirectUri == null) {
            return Mono.just(request);
        }
        return exchange.getSession()
                .doOnNext(session -> session.getAttributes().put(SESSION_KEY, redirectUri))
                .thenReturn(request);
    }
}

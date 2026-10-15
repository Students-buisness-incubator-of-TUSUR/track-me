package net.trackme.cliengateway.config;

import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class CustomAuthorizationRequestResolver implements ServerOAuth2AuthorizationRequestResolver {

    private static final String YANDEX = "yandex";
    static final String SESSION_KEY = "post_login_redirect_uri";

    private final DefaultServerOAuth2AuthorizationRequestResolver delegate;
    private final boolean yandexSsoEnabled;

    public CustomAuthorizationRequestResolver(ReactiveClientRegistrationRepository repository) {
        this(repository, false);
    }

    public CustomAuthorizationRequestResolver(ReactiveClientRegistrationRepository repository, boolean enabled) {
        this.delegate = new DefaultServerOAuth2AuthorizationRequestResolver(repository);
        this.yandexSsoEnabled = enabled;
    }

    @Override
    public Mono<OAuth2AuthorizationRequest> resolve(ServerWebExchange exchange) {
        if (yandexSsoEnabled && "/oauth2/authorization/yandex".equals(exchange.getRequest().getPath().value())) {
            return resolve(exchange, YANDEX);
        }
        return delegate.resolve(exchange)
                .flatMap(request -> saveRedirectUri(exchange, request));
    }

    @Override
    public Mono<OAuth2AuthorizationRequest> resolve(ServerWebExchange exchange, String clientRegistrationId) {
        if (yandexSsoEnabled && YANDEX.equals(clientRegistrationId)) {
            return delegate.resolve(exchange, "track-me-client")
                    .map(request -> OAuth2AuthorizationRequest.from(request)
                            .additionalParameters(parameters -> parameters.put("login_hint", YANDEX))
                            .build())
                    .flatMap(request -> saveRedirectUri(exchange, request));
        }
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

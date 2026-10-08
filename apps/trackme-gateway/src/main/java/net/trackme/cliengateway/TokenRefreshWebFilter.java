package net.trackme.cliengateway;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Refreshes tokens once for concurrent requests belonging to the same gateway session. */
@Component
@RequiredArgsConstructor
public class TokenRefreshWebFilter implements WebFilter {
    private final ReactiveOAuth2AuthorizedClientManager authorizedClientManager;
    private final ServerOAuth2AuthorizedClientRepository authorizedClientRepository;
    private final Map<String, Mono<OAuth2AuthorizedClient>> refreshes = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return exchange.getPrincipal().ofType(OAuth2AuthenticationToken.class)
                .flatMap(principal -> exchange.getSession().flatMap(session -> {
                    String key = session.getId() + ":" + principal.getAuthorizedClientRegistrationId();
                    var refresh = refreshes.computeIfAbsent(key, ignored -> Mono.defer(() -> {
                        var request = OAuth2AuthorizeRequest
                                .withClientRegistrationId(principal.getAuthorizedClientRegistrationId())
                                .principal(principal)
                                .attribute(ServerWebExchange.class.getName(), exchange)
                                .build();
                        return authorizedClientManager.authorize(request);
                    }).doFinally(signal -> Schedulers.parallel().schedule(
                            () -> refreshes.remove(key), 5, TimeUnit.SECONDS)).cache());
                    // Save into every request's session, including followers of a shared refresh.
                    return refresh.flatMap(client -> authorizedClientRepository
                            .saveAuthorizedClient(client, principal, exchange));
                }))
                .then(Mono.defer(() -> chain.filter(exchange)));
    }
}

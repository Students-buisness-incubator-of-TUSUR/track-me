package net.trackme.cliengateway.controller;

import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class CsrfController {

    @GetMapping("/csrf")
    public Mono<CsrfTokenResponse> csrf(ServerWebExchange exchange) {
        Mono<CsrfToken> csrfToken = exchange.getAttribute(CsrfToken.class.getName());
        if (csrfToken == null) {
            return Mono.empty();
        }
        return csrfToken.map(
                token -> new CsrfTokenResponse(token.getToken(), token.getHeaderName()));
    }

    public record CsrfTokenResponse(String token, String headerName) {
    }
}

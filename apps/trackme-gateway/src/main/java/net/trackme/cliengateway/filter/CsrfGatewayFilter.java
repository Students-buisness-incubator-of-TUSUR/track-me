package net.trackme.cliengateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Глобальный фильтр для обработки CSRF токенов в Gateway
 */
@Component
public class CsrfGatewayFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Исключаем из CSRF проверки запросы к внутренним API через Gateway


        // Для всех запросов просто продолжаем цепочку фильтров
        // CSRF защита уже настроена на уровне SecurityWebFilterChain
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // Выполняем после CSRF фильтра Spring Security
        return -99;
    }
}

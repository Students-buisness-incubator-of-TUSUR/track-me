# CSRF Protection Configuration

## Обзор

В Gateway настроена CSRF защита для предотвращения атак Cross-Site Request Forgery. CSRF защита применяется только к
критическим операциям Gateway (logout, OAuth2 аутентификация), в то время как запросы к внутренним API проксируются без
CSRF проверки.

## Архитектура безопасности

```
Frontend (React) 
    ↓ (включает CSRF токен в запросы)
Gateway (Spring Cloud Gateway + OAuth2 Client)
    ↓ (проксирует запросы с JWT токенами)
Internal Services (Resource Servers)
```

### Gateway (OAuth2 Client)

- **CSRF**: включен для `/logout`, `/oauth2/**`
- **Session Management**: cookie-based сессии
- **Аутентификация**: OAuth2/OIDC
- **Проксирование**: передает JWT токены внутренним сервисам

### Internal Services (Resource Servers)

- **CSRF**: отключен (получают JWT токены от Gateway)
- **Аутентификация**: JWT token validation
- **Stateless**: не используют сессии

## Конфигурация CSRF в Gateway

### 1. Security Configuration

```java

@Bean
SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
    http
            .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
                    .requireCsrfProtectionMatcher(ServerWebExchangeMatchers.pathMatchers(
                            "/logout",
                            "/oauth2/**"
                    ))
            )
    // ...остальная конфигурация
}
```

### 2. CSRF Token Endpoint

Gateway предоставляет endpoint `/csrf` для получения CSRF токена:

```bash
GET /csrf
Response: {"token": "xxx", "headerName": "X-CSRF-TOKEN"}
```

### 3. Frontend Integration

#### Получение CSRF токена

```javascript
const getCsrfToken = async () => {
    const response = await fetch('/csrf', {
        credentials: 'include'
    });
    return await response.json();
};
```

#### Использование в запросах

```javascript
const {token, headerName} = await getCsrfToken();

fetch('/logout', {
    method: 'POST',
    credentials: 'include',
    headers: {
        [headerName]: token
    }
});
```

## Environment Variables

```bash
# Cookie настройки
SESSION_COOKIE_SAME_SITE=Lax
SESSION_COOKIE_SECURE=true
SESSION_COOKIE_DOMAIN=
```

## Почему внутренние сервисы не нуждаются в CSRF

1. **Stateless архитектура**: внутренние сервисы не используют cookie-based аутентификацию
2. **JWT токены**: аутентификация через Bearer токены
3. **Внутренняя сеть**: сервисы доступны только через Gateway
4. **Same-Origin Policy**: браузер не может напрямую обращаться к внутренним сервисам

## Рекомендации

### Для Production

- Установите `SESSION_COOKIE_SECURE=true`
- Настройте правильный домен в `SESSION_COOKIE_DOMAIN`
- Используйте HTTPS

### Для Development

- `SESSION_COOKIE_SECURE=false` (для HTTP)
- `SESSION_COOKIE_SAME_SITE=Lax`

## Troubleshooting

### CSRF Token Missing

- Проверьте, что фронтенд получает токен через `/csrf`
- Убедитесь, что токен передается в правильном заголовке

### Cookie Not Set

- Проверьте настройки домена cookie
- Убедитесь, что `credentials: 'include'` используется в fetch запросах

### CORS Issues

- Проверьте конфигурацию `allowCredentials: true` в CORS
- Убедитесь, что фронтенд домен в списке `allowedOrigins`

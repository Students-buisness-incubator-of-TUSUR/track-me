# CORS Configuration Guide

## Overview

This document describes the CORS (Cross-Origin Resource Sharing) configuration for the TrackMe Gateway module and provides guidance on how to configure it properly for different environments.

## Key Improvements

### 1. Removed Wildcard Headers
**Before:** `allowed-headers: *`
**After:** `allowed-headers: Authorization,Content-Type,Accept,Origin,X-Requested-With,X-CSRF-TOKEN`

**Reason:** When `allow-credentials: true`, using wildcard headers (`*`) is overly permissive and can be rejected by browsers. Explicit headers are more secure and comply with CORS specifications for credentialed requests.

### 2. Added Exposed Headers
**New:** `exposed-headers: X-CSRF-TOKEN,Authorization`

**Reason:** Frontend applications need access to custom response headers like CSRF tokens and authorization headers. Without explicitly exposing them, JavaScript code cannot read these headers from the response.

### 3. Support for Origin Patterns
**New:** `allowed-origin-patterns` alongside `allowed-origins`

**Reason:** 
- **Development:** Origin patterns (e.g., `http://localhost:*`) allow flexible local development on any port
- **Production:** Explicit origins provide strict security by whitelisting only known frontend domains

### 4. Environment-Specific Configuration

#### Default (Development)
- Uses `allowed-origin-patterns` for flexibility: `http://localhost:*`, `http://127.0.0.1:*`
- Logging level: `info`
- Session cookies: `secure: false`, `same-site: Lax`
- Swagger UI: enabled

#### Production Profile
- **MUST** use explicit `allowed-origins` (configured via `CORS_ORIGINS` env var)
- No origin patterns allowed in production
- Logging level: `warn` for security, `info` for gateway
- Session cookies: `secure: true`, `same-site: Strict`
- Swagger UI: disabled by default (can be enabled with `SWAGGER_ENABLED=true`)

### 5. Fixed OAuth2 Configuration Issues

#### Docker-Local Profile
**Before:**
```yaml
issuer-uri: http://host.docker.internal:9000
authorization-uri: http://localhost:9000/oauth2/authorize  # ❌ Inconsistent
```

**After:**
```yaml
issuer-uri: http://host.docker.internal:9000
authorization-uri: http://host.docker.internal:9000/oauth2/authorize  # ✅ Consistent
```

**Reason:** Mixing `host.docker.internal` and `localhost` causes OAuth2 discovery and token validation issues in Docker environments.

#### Swagger URL Typo
**Before:** `http://127.0.1:8081/meeting/v3/api-docs` (missing `.0`)
**After:** `http://127.0.0.1:8081/meeting/v3/api-docs`

### 6. Improved Logging Configuration
**Before:** `org.springframework.security: trace` (always)
**After:** `org.springframework.security: ${SECURITY_LOG_LEVEL:info}`

**Reason:** Trace-level security logging is too verbose for production and can impact performance. Now configurable via environment variable.

## Configuration

### Environment Variables

| Variable | Default (Dev) | Production | Description |
|----------|---------------|------------|-------------|
| `CORS_ORIGINS` | (empty) | **Required** | Comma-separated list of exact allowed origins |
| `CORS_ORIGIN_PATTERNS` | `http://localhost:*,http://127.0.0.1:*` | (empty) | Origin patterns for flexible matching |
| `CORS_ALLOWED_HEADERS` | `Authorization,Content-Type,...` | Same | Specific headers allowed in requests |
| `CORS_EXPOSED_HEADERS` | `X-CSRF-TOKEN,Authorization` | Same | Headers exposed to frontend |
| `CORS_ALLOWED_METHODS` | `GET,POST,PUT,DELETE,OPTIONS,PATCH` | Same | HTTP methods allowed |
| `CORS_ALLOW_CREDENTIALS` | `true` | `true` | Allow cookies/credentials |
| `SESSION_COOKIE_SECURE` | `false` | `true` | Require HTTPS for cookies |
| `SESSION_COOKIE_SAME_SITE` | `Lax` | `Strict` | Cookie same-site policy |
| `SECURITY_LOG_LEVEL` | `info` | `warn` | Spring Security log level |
| `GATEWAY_LOG_LEVEL` | `info` | `info` | Gateway log level |
| `SWAGGER_ENABLED` | `true` | `false` | Enable Swagger UI |

### Example Configurations

#### Local Development (default)
```bash
# No environment variables needed - uses sensible defaults
./gradlew :trackme-gateway:bootRun
```

#### Docker Local (docker-compose)
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=docker-local
  - SESSION_COOKIE_SAME_SITE=None  # For cross-domain in Docker
  - SESSION_COOKIE_SECURE=True
  - CORS_ORIGINS=http://localhost:3000,http://127.0.0.1:3000
```

#### Production
```bash
export SPRING_PROFILES_ACTIVE=production
export CORS_ORIGINS=https://trackme.example.com,https://app.example.com
export SESSION_COOKIE_DOMAIN=.example.com
export SESSION_COOKIE_SECURE=true
export SESSION_COOKIE_SAME_SITE=Strict
export SECURITY_LOG_LEVEL=warn
export SWAGGER_ENABLED=false
```

## Security Best Practices

### ✅ DO:
1. Use explicit `allowed-origins` in production
2. Keep `allow-credentials: true` only if you need cookie-based auth
3. List specific headers instead of wildcards
4. Use `Strict` same-site cookies in production
5. Require HTTPS (`secure: true`) in production
6. Disable Swagger UI in production

### ❌ DON'T:
1. Use `allowed-origins: *` with `allow-credentials: true` (will fail in browsers)
2. Use `allowed-headers: *` with credentials enabled
3. Use origin patterns in production
4. Mix `localhost` and `host.docker.internal` in OAuth2 configuration
5. Keep trace-level logging in production
6. Use `same-site: None` without `secure: true`

## Testing CORS

### Using curl
```bash
# Preflight request
curl -X OPTIONS http://localhost:8081/backend/api/endpoint \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type,Authorization" \
  -v

# Actual request
curl -X POST http://localhost:8081/backend/api/endpoint \
  -H "Origin: http://localhost:3000" \
  -H "Content-Type: application/json" \
  -b "SESSION=xxx" \
  -d '{"test": "data"}' \
  -v
```

### Expected Response Headers
```
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Credentials: true
Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS,PATCH
Access-Control-Allow-Headers: Authorization,Content-Type,Accept,Origin,X-Requested-With,X-CSRF-TOKEN
Access-Control-Expose-Headers: X-CSRF-TOKEN,Authorization
Access-Control-Max-Age: 3600
```

## Troubleshooting

### Issue: CORS errors in browser console
**Symptom:** `No 'Access-Control-Allow-Origin' header is present`
**Solution:** 
- Verify origin is in `CORS_ORIGINS` or matches `CORS_ORIGIN_PATTERNS`
- Check browser DevTools Network tab for preflight (OPTIONS) request status

### Issue: Credentials not being sent
**Symptom:** Cookies not included in requests
**Solution:**
- Ensure `allow-credentials: true` in gateway config
- Frontend must use `credentials: 'include'` in fetch/axios
- Origin must be explicit (not `*`)

### Issue: Custom headers not readable in frontend
**Symptom:** `response.headers.get('X-CSRF-TOKEN')` returns null
**Solution:** Add header name to `exposed-headers` configuration

### Issue: OAuth2 redirects fail in Docker
**Symptom:** Token validation errors or redirect loops
**Solution:** Use consistent `host.docker.internal` or `localhost` throughout OAuth2 config, not mixed

## Architecture

```
┌─────────────┐      CORS        ┌──────────────────┐
│   Browser   │ ◄─────────────► │  Gateway :8081   │
│ (Frontend)  │   credentials    │                  │
└─────────────┘                  │  - CorsWebFilter │
                                 │  - OAuth2 Client │
                                 └────────┬─────────┘
                                          │ TokenRelay
                          ┌───────────────┼───────────────┐
                          ▼               ▼               ▼
                   ┌─────────────┐ ┌──────────┐ ┌────────────┐
                   │   Backend   │ │ Meeting  │ │    SSO     │
                   │    :8080    │ │  :8082   │ │   :9000    │
                   └─────────────┘ └──────────┘ └────────────┘
```

The gateway handles:
1. **CORS** for cross-origin requests from frontend
2. **Session management** with Redis-backed HTTP sessions
3. **OAuth2 authentication** with SSO service
4. **Token relay** to propagate access tokens to backend services

## References

- [MDN CORS Documentation](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)
- [Spring Security CORS](https://docs.spring.io/spring-security/reference/reactive/integrations/cors.html)
- [Spring Cloud Gateway CORS](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway/cors-configuration.html)

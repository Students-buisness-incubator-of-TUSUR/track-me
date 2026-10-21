package net.trackme.sso.services.social;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import net.trackme.sso.dto.AuthorizedUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SocialLoginHandler implements AuthenticationSuccessHandler {
    public static final String PENDING = "trackme.yandex.pending";
    private final SocialAccountService accounts;
    private final HttpSessionSecurityContextRepository contexts = new HttpSessionSecurityContextRepository();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        // An external identity alone must never authorize calls to TrackMe APIs.
        clear(request, response);
        request.getSession().removeAttribute(PENDING);
        request.getSession().removeAttribute("trackme.yandex.error");
        request.getSession().removeAttribute("trackme.yandex.fieldErrors");
        request.getSession().removeAttribute("trackme.yandex.formValues");
        try {
            var attributes = ((OAuth2User) authentication.getPrincipal()).getAttributes();
            boolean google = authentication instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauth
                    && "google".equals(oauth.getAuthorizedClientRegistrationId());
            var profile = google ? SocialProfile.google(attributes, (String) attributes.get("trackme_phone"))
                    : SocialProfile.from(attributes);
            var user = accounts.login(profile);
            if (user.isPresent()) {
                complete(request, response, user.get());
            } else {
                request.getSession().setAttribute(PENDING, profile);
                request.getSession().setAttribute("trackme.yandex.linkAttempts", 0);
                response.sendRedirect(request.getContextPath() + "/client/yandex/complete");
            }
        } catch (IllegalArgumentException exception) {
            request.getSession().setAttribute("trackme.yandex.error", exception.getMessage());
            response.sendRedirect(request.getContextPath() + "/client/yandex/complete");
        }
    }

    public void complete(HttpServletRequest request, HttpServletResponse response, AuthorizedUser user)
            throws IOException {
        request.changeSessionId();
        var authentication = UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        request.getSession().removeAttribute(PENDING);
        var cache = new HttpSessionRequestCache();
        var saved = cache.getRequest(request, response);
        cache.removeRequest(request, response);
        // Resume only our authorization endpoint, never the cached host or redirect URL.
        if (!(saved instanceof DefaultSavedRequest original) || !"GET".equals(original.getMethod())
                || !"/oauth2/authorize".equals(original.getRequestURI())) {
            response.sendRedirect("/client/login");
            return;
        }
        var parameters = new ArrayList<String>();
        saved.getParameterMap().forEach((name, values) -> {
            for (String value : values) {
                parameters.add(URLEncoder.encode(name, StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        });
        // All query data is encoded; the authorization server validates client redirect_uri.
        response.sendRedirect("/oauth2/authorize?" + String.join("&", parameters));
    }

    private void clear(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        contexts.saveContext(SecurityContextHolder.createEmptyContext(), request, response);
    }
}

package net.trackme.sso.services.yandex;

import jakarta.servlet.ServletException;
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
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class YandexLoginHandler implements AuthenticationSuccessHandler {
    public static final String PENDING = "trackme.yandex.pending";
    private final YandexAccountService accounts;
    private final HttpSessionSecurityContextRepository contexts = new HttpSessionSecurityContextRepository();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // An external identity alone must never authorize calls to TrackMe APIs.
        clear(request, response);
        request.getSession().removeAttribute(PENDING);
        try {
            var profile = YandexProfile.from(((OAuth2User) authentication.getPrincipal()).getAttributes());
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
            throws IOException, ServletException {
        request.changeSessionId();
        var authentication = UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        request.getSession().removeAttribute(PENDING);
        var success = new SavedRequestAwareAuthenticationSuccessHandler();
        success.setDefaultTargetUrl("/client/login");
        success.onAuthenticationSuccess(request, response, authentication);
    }

    private void clear(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        contexts.saveContext(SecurityContextHolder.createEmptyContext(), request, response);
    }
}

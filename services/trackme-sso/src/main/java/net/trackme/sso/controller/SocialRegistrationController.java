package net.trackme.sso.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import net.trackme.sso.services.social.SocialAccountService;
import net.trackme.sso.services.social.SocialLoginHandler;
import net.trackme.sso.services.social.SocialProfile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("${app.yandex.enabled:false} or ${app.google.enabled:false}")
public class SocialRegistrationController {
    private static final String COMPLETE_REDIRECT = "redirect:/client/yandex/complete";
    private static final String REGISTERED = "yandex.registered";
    private static final String USERNAME = "username";
    private static final String ERROR = "trackme.yandex.error";
    private static final String FIELD_ERRORS = "trackme.yandex.fieldErrors";
    private static final String FORM_VALUES = "trackme.yandex.formValues";
    private final SocialAccountService accounts;
    private final SocialLoginHandler login;

    @org.springframework.beans.factory.annotation.Value("${FRONTEND_URI:http://localhost}")
    private String frontendUri;

    @GetMapping({"/client/yandex/complete", "/client/social/complete"})
    public String form(HttpServletRequest request, Model model) {
        if (request.getParameter("error") != null) {
            request.getSession().setAttribute(ERROR, "Провайдер не подтвердил вход. Повторите попытку.");
        }
        return "redirect:" + org.springframework.util.StringUtils.trimTrailingCharacter(frontendUri, '/') + "/social-registration";
    }

    @GetMapping({"/client/yandex/profile", "/client/social/profile"})
    @org.springframework.web.bind.annotation.ResponseBody
    public Map<String, Object> profile(HttpServletRequest request,
            org.springframework.security.web.csrf.CsrfToken csrf) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("csrfToken", csrf.getToken());
        result.put("csrfHeader", csrf.getHeaderName());
        Object error = request.getSession().getAttribute(ERROR);
        if (error != null) result.put("message", error);
        SocialProfile value = SocialProfile.pending(request.getSession().getAttribute(SocialLoginHandler.PENDING));
        boolean valid = value != null && java.time.Instant.now().isBefore(value.expiresAt());
        result.put("pending", valid);
        if (valid) {
            result.put("provider", value.provider());
            result.put("phoneNumber", value.phoneNumber());
        }
        if (valid) result.put("email", value.email());
        return result;
    }

    public record Registration(String username, String role, boolean licenseAccepted,
                               boolean policyAccepted, boolean consent, String phoneNumber) {
        public Registration(String username, String role, boolean licenseAccepted, boolean policyAccepted, boolean consent) {
            this(username, role, licenseAccepted, policyAccepted, consent, null);
        }
    }

    @PostMapping({"/client/yandex/registration", "/client/social/registration"})
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<Map<String, Object>> registration(
            HttpServletRequest request,
            @org.springframework.web.bind.annotation.RequestBody Registration body) {
        // The same validation is shared by API and legacy POST; all identity data stays in the session.
        register(request, body.username() == null ? "" : body.username(),
                body.role() == null ? "" : body.role(), body.licenseAccepted(), body.policyAccepted(), body.consent(), body.phoneNumber());
        Object fields = request.getSession().getAttribute(FIELD_ERRORS);
        if (fields != null) {
            request.getSession().removeAttribute(FIELD_ERRORS);
            return org.springframework.http.ResponseEntity.badRequest().body(Map.of("fieldErrors", fields));
        }
        Object message = request.getSession().getAttribute(ERROR);
        boolean registered = Boolean.TRUE.equals(request.getAttribute(REGISTERED));
        return org.springframework.http.ResponseEntity.status(registered ? 201 : 400)
                .body(Map.of("registered", registered, "message", message == null ? "Повторите вход через Google или Яндекс." : message));
    }

    @PostMapping("/client/yandex/register")
    public String register(HttpServletRequest request, @RequestParam(defaultValue = "") String username,
                           @RequestParam(defaultValue = "") String role,
                           @RequestParam(defaultValue = "false") boolean licenseAccepted,
                           @RequestParam(defaultValue = "false") boolean policyAccepted,
                           @RequestParam(defaultValue = "false") boolean consent) {
        return register(request, username, role, licenseAccepted, policyAccepted, consent, null);
    }

    private String register(HttpServletRequest request, String username, String role,
                            boolean licenseAccepted, boolean policyAccepted, boolean consent, String phoneNumber) {
        request.removeAttribute(REGISTERED);
        request.getSession().removeAttribute(FIELD_ERRORS);
        request.getSession().removeAttribute(ERROR);
        username = SocialAccountService.normalizeUsername(username);
        Map<String, String> fields = new LinkedHashMap<>();
        if (!username.matches("[a-zA-Z0-9._-]{6,100}")) {
            fields.put(USERNAME, "Введите от 6 до 100 символов: латинские буквы, цифры, _, . или -.");
        }
        if (!Set.of("TRACKER", "ADMIN").contains(role)) fields.put("role", "Выберите роль.");
        if (!licenseAccepted) fields.put("licenseAccepted", "Примите условия лицензионного соглашения.");
        if (!policyAccepted) fields.put("policyAccepted", "Подтвердите ознакомление с политикой.");
        if (!consent) fields.put("consent", "Необходимо согласие на обработку персональных данных.");
        if (phoneNumber != null && !phoneNumber.isBlank() && !phoneNumber.matches("\\+?\\d{10,15}")) {
            fields.put("phoneNumber", "Введите от 10 до 15 цифр; в начале допустим +.");
        }
        if (!fields.isEmpty()) {
            request.getSession().setAttribute(FIELD_ERRORS, fields);
            request.getSession().setAttribute(FORM_VALUES, Map.of(USERNAME, username, "role", role));
            return COMPLETE_REDIRECT;
        }
        try {
            if (phoneNumber == null || phoneNumber.isBlank()) accounts.register(pending(request), username, role);
            else accounts.register(pending(request), username, role, phoneNumber);
            request.setAttribute(REGISTERED, true);
            request.getSession().removeAttribute(SocialLoginHandler.PENDING);
            request.getSession().setAttribute(ERROR,
                    "Регистрация завершена. После активации аккаунта администратором вы сможете входить через Google или Яндекс.");
        } catch (IllegalArgumentException | DataIntegrityViolationException exception) {
            request.getSession().setAttribute(FORM_VALUES, Map.of(USERNAME, username, "role", role));
            error(request, exception);
        }
        return COMPLETE_REDIRECT;
    }

    @PostMapping({"/client/yandex/link", "/client/social/link"})
    public String link(HttpServletRequest request, HttpServletResponse response,
                       @RequestParam String username, @RequestParam String password) throws IOException {
        try {
            var profile = pending(request);
            Integer attempts = (Integer) request.getSession().getAttribute("trackme.yandex.linkAttempts");
            if (attempts == null || attempts >= 5) {
                request.getSession().removeAttribute(SocialLoginHandler.PENDING);
                throw new IllegalArgumentException("Слишком много попыток. Войдите через Google или Яндекс заново.");
            }
            request.getSession().setAttribute("trackme.yandex.linkAttempts", attempts + 1);
            login.complete(request, response, accounts.link(profile, username, password));
            return null;
        } catch (IllegalArgumentException | DataIntegrityViolationException exception) {
            error(request, exception);
            return COMPLETE_REDIRECT;
        }
    }

    private SocialProfile pending(HttpServletRequest request) {
        SocialProfile value = SocialProfile.pending(request.getSession().getAttribute(SocialLoginHandler.PENDING));
        if (value == null) {
            throw new IllegalArgumentException("Сначала подтвердите вход через Google или Яндекс.");
        }
        value.requireValid();
        return value;
    }

    private void error(HttpServletRequest request, RuntimeException exception) {
        request.getSession().setAttribute(ERROR, exception instanceof IllegalArgumentException
                ? exception.getMessage() : "Аккаунт уже существует. Повторите вход или привяжите существующий аккаунт.");
    }
}

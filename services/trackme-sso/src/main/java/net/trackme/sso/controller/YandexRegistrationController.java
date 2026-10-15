package net.trackme.sso.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import net.trackme.sso.services.yandex.YandexAccountService;
import net.trackme.sso.services.yandex.YandexLoginHandler;
import net.trackme.sso.services.yandex.YandexProfile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.yandex.enabled", havingValue = "true")
public class YandexRegistrationController {
    private static final String ERROR = "trackme.yandex.error";
    private static final String FIELD_ERRORS = "trackme.yandex.fieldErrors";
    private static final String FORM_VALUES = "trackme.yandex.formValues";
    private final YandexAccountService accounts;
    private final YandexLoginHandler login;

    @GetMapping("/client/yandex/complete")
    public String form(HttpServletRequest request, Model model) {
        var session = request.getSession();
        Object error = session.getAttribute(ERROR);
        session.removeAttribute(ERROR);
        if (error == null && request.getParameter("error") != null) {
            error = "Яндекс не подтвердил вход. Повторите попытку.";
        }
        model.addAttribute("error", error);
        Object fields = session.getAttribute(FIELD_ERRORS);
        Object values = session.getAttribute(FORM_VALUES);
        session.removeAttribute(FIELD_ERRORS);
        session.removeAttribute(FORM_VALUES);
        model.addAttribute("fieldErrors", fields instanceof Map ? fields : Map.of());
        model.addAttribute("values", values instanceof Map ? values : Map.of());
        Object pending = session.getAttribute(YandexLoginHandler.PENDING);
        model.addAttribute("profile", pending instanceof YandexProfile profile
                && java.time.Instant.now().isBefore(profile.expiresAt()) ? profile : null);
        return "yandex-registration";
    }

    @PostMapping("/client/yandex/register")
    public String register(HttpServletRequest request, @RequestParam(defaultValue = "") String username,
                           @RequestParam(defaultValue = "") String role,
                           @RequestParam(defaultValue = "false") boolean licenseAccepted,
                           @RequestParam(defaultValue = "false") boolean policyAccepted,
                           @RequestParam(defaultValue = "false") boolean consent) {
        username = YandexAccountService.normalizeUsername(username);
        Map<String, String> fields = new LinkedHashMap<>();
        if (!username.matches("[a-zA-Z0-9._-]{6,100}")) {
            fields.put("username", "Введите от 6 до 100 символов: латинские буквы, цифры, _, . или -.");
        }
        if (!Set.of("TRACKER", "ADMIN").contains(role)) fields.put("role", "Выберите роль.");
        if (!licenseAccepted) fields.put("licenseAccepted", "Примите условия лицензионного соглашения.");
        if (!policyAccepted) fields.put("policyAccepted", "Подтвердите ознакомление с политикой.");
        if (!consent) fields.put("consent", "Необходимо согласие на обработку персональных данных.");
        if (!fields.isEmpty()) {
            request.getSession().setAttribute(FIELD_ERRORS, fields);
            request.getSession().setAttribute(FORM_VALUES, Map.of("username", username, "role", role));
            return "redirect:/client/yandex/complete";
        }
        try {
            accounts.register(pending(request), username, role);
            request.getSession().removeAttribute(YandexLoginHandler.PENDING);
            request.getSession().setAttribute(ERROR,
                    "Регистрация завершена. После активации аккаунта администратором вы сможете входить через Яндекс.");
        } catch (IllegalArgumentException | DataIntegrityViolationException exception) {
            request.getSession().setAttribute(FORM_VALUES, Map.of("username", username, "role", role));
            error(request, exception);
        }
        return "redirect:/client/yandex/complete";
    }

    @PostMapping("/client/yandex/link")
    public String link(HttpServletRequest request, HttpServletResponse response,
                       @RequestParam String username, @RequestParam String password) throws IOException, ServletException {
        try {
            var profile = pending(request);
            Integer attempts = (Integer) request.getSession().getAttribute("trackme.yandex.linkAttempts");
            if (attempts == null || attempts >= 5) {
                request.getSession().removeAttribute(YandexLoginHandler.PENDING);
                throw new IllegalArgumentException("Слишком много попыток. Войдите через Яндекс заново.");
            }
            request.getSession().setAttribute("trackme.yandex.linkAttempts", attempts + 1);
            login.complete(request, response, accounts.link(profile, username, password));
            return null;
        } catch (IllegalArgumentException | DataIntegrityViolationException exception) {
            error(request, exception);
            return "redirect:/client/yandex/complete";
        }
    }

    private YandexProfile pending(HttpServletRequest request) {
        Object value = request.getSession().getAttribute(YandexLoginHandler.PENDING);
        if (!(value instanceof YandexProfile profile)) {
            throw new IllegalArgumentException("Сначала подтвердите вход через Яндекс.");
        }
        profile.requireValid();
        return profile;
    }

    private void error(HttpServletRequest request, RuntimeException exception) {
        request.getSession().setAttribute(ERROR, exception instanceof IllegalArgumentException
                ? exception.getMessage() : "Аккаунт уже существует. Повторите вход или привяжите существующий аккаунт.");
    }
}

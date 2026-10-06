package net.trackme.sso.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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
    private final YandexAccountService accounts;
    private final YandexLoginHandler login;

    @GetMapping("/client/yandex/complete")
    public String form(HttpServletRequest request, Model model) {
        var session = request.getSession();
        Object error = session.getAttribute("trackme.yandex.error");
        session.removeAttribute("trackme.yandex.error");
        model.addAttribute("error", error != null ? error : request.getParameter("error") != null
                ? "Яндекс не подтвердил вход. Повторите попытку." : null);
        Object pending = session.getAttribute(YandexLoginHandler.PENDING);
        model.addAttribute("profile", pending instanceof YandexProfile profile
                && java.time.Instant.now().isBefore(profile.expiresAt()) ? profile : null);
        return "yandex-registration";
    }

    @PostMapping("/client/yandex/register")
    public String register(HttpServletRequest request, @RequestParam String username,
                           @RequestParam String phone, @RequestParam String role,
                           @RequestParam(defaultValue = "false") boolean consent) {
        try {
            if (!consent) throw new IllegalArgumentException("Необходимо согласие на обработку персональных данных.");
            accounts.register(pending(request), username, phone, role);
            request.getSession().removeAttribute(YandexLoginHandler.PENDING);
            request.getSession().setAttribute("trackme.yandex.error",
                    "Регистрация завершена. После активации аккаунта администратором вы сможете входить через Яндекс.");
        } catch (IllegalArgumentException | DataIntegrityViolationException exception) {
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
        request.getSession().setAttribute("trackme.yandex.error", exception instanceof IllegalArgumentException
                ? exception.getMessage() : "Аккаунт уже существует. Повторите вход или привяжите существующий аккаунт.");
    }
}

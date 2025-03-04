package net.akarmanov.projectplace.services.reset;

import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.akarmanov.projectplace.configuration.AppProperties;
import net.akarmanov.projectplace.domain.PasswordResetToken;
import net.akarmanov.projectplace.domain.User;
import net.akarmanov.projectplace.repos.PasswordResetRepository;
import net.akarmanov.projectplace.repos.UserRepository;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Instant;
import java.util.UUID;

import static java.util.concurrent.CompletableFuture.runAsync;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class MailPasswordResetService implements PasswordResetService {

  private final PasswordResetRepository passwordResetRepository;

  private final UserRepository userRepository;

  private final JavaMailSender mailSender;

  private final PasswordEncoder passwordEncoder;

  private final AppProperties appProperties;

  private final TemplateEngine templateEngine;

  @Override
  public void createToken(User user) {
    var token = UUID.randomUUID().toString();
    var passwordResetToken = new PasswordResetToken(token, user);
    passwordResetRepository.save(passwordResetToken);
    log.info("Создан токен сброса пароля для пользователя: {}", user.getEmail());

    runAsync(() -> sendEmail(user.getEmail(), token));
  }

  @Override
  public void validateToken(String token) {
    var passwordResetToken = passwordResetRepository.findByToken(token)
        .orElseThrow(() -> new InvalidTokenException(token));
    if (passwordResetToken.getExpiryDate().isBefore(Instant.now())) {
      throw new ExpiredTokenException(token);
    }
  }

  @Override
  public void resetPassword(String token, String newPassword) {
    var passwordResetToken = passwordResetRepository.findByToken(token)
        .orElseThrow(() -> new InvalidTokenException(token));
    var user = passwordResetToken.getUser();
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);
    passwordResetRepository.delete(passwordResetToken);
    log.info("Пароль сброшен для пользователя: {}", user.getEmail());
  }

  @Scheduled(cron = "${app.password-reset.token-expiration-check-cron:0 0 * * * *}")
  public void deleteExpiredTokens() {
    var now = Instant.now();
    passwordResetRepository.deleteAllByExpiryDateBefore(now);
    log.info("Удалены все токены сброса пароля, срок действия которых истек до: {}", now);
  }

  public void sendEmail(@Email String email, String token) {
    try {
      log.info("Отправка email на адрес: {}", email);
      var message = mailSender.createMimeMessage();
      var helper = new MimeMessageHelper(message, true, "UTF-8");

      var content = buildContent(token, email);
      helper.setTo(email);
      helper.setFrom(appProperties.getMail().getFrom());
      helper.setSubject("[" + appProperties.getName() + "] Сброс пароля");
      helper.setText(content, true);
      mailSender.send(message);
      log.info("Email успешно отправлен на адрес: {}", email);
    } catch (Exception e) {
      log.error("Ошибка при отправке email на адрес: {}", email, e);
    }
  }

  private String buildContent(String token, @Email String email) {
    var context = new Context();
    context.setVariable("email", email);
    context.setVariable("resetLink", buildResetLink(token));
    context.setVariable("baseUrl", appProperties.getAppUrl());
    context.setVariable("appName", appProperties.getName());
    context.setVariable("supportEmail", appProperties.getMail().getFrom());
    return templateEngine.process("password-reset-email.html", context);
  }

  private String buildResetLink(String token) {
    return UriComponentsBuilder
        .fromUriString(appProperties.getAppUrl(), UriComponentsBuilder.ParserType.WHAT_WG)
        .path("/reset-password")
        .queryParam("token", token)
        .toUriString();
  }

}

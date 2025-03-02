package net.akarmanov.projectplace.services.reset;

import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.akarmanov.projectplace.configuration.AppProperties;
import net.akarmanov.projectplace.domain.PasswordResetToken;
import net.akarmanov.projectplace.domain.User;
import net.akarmanov.projectplace.repos.PasswordResetRepository;
import net.akarmanov.projectplace.repos.UserRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.util.UriComponentsBuilder;

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
      var message = new SimpleMailMessage();
      message.setTo(email);
      message.setSubject("Сброс пароля");
      message.setText(buildEmailBody(token));
      message.setFrom(appProperties.getMail().getFrom());
      mailSender.send(message);
      log.info("Email успешно отправлен на адрес: {}", email);
    } catch (Exception e) {
      log.error("Ошибка при отправке email на адрес: {}", email, e);
    }
  }

  private String buildEmailBody(String token) {
    var uriBuilder = UriComponentsBuilder
        .fromUriString(appProperties.getAppUrl())
        .path("/reset-password")
        .queryParam("token", token);
    return """
        <html>
            <body>
                <p>Для сброса пароля перейдите по ссылке:</p>
                <a href="%s">Сбросить пароль</a>
                <p>Ссылка действительна 1 час.</p>
            </body>
        </html>
        """.formatted(uriBuilder.toUriString());
  }
}

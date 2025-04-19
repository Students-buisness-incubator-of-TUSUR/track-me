package net.akarmanov.projectplace.sso.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.akarmanov.projectplace.sso.config.AppProperties;
import net.akarmanov.projectplace.sso.config.security.AuthorizationServerProperties;
import net.akarmanov.projectplace.sso.services.MailService;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder.ParserType;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static java.util.concurrent.CompletableFuture.runAsync;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultMailService implements MailService {

  private final JavaMailSender mailSender;

  private final TemplateEngine templateEngine;

  private final AuthorizationServerProperties authorizationServerProperties;

  private final AppProperties appProperties;

  @Override
  public void sendRegistrationConfirmationEmail(String email, String token) {
    runAsync(() -> sendConfirmationEmail(email, token));
  }

  private void sendConfirmationEmail(String email, String token) {
    var content = generateEmailContent(email, token);
    try {
      log.info("Отправка email на адрес: {}", email);
      var message = mailSender.createMimeMessage();
      var helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setTo(email);
      helper.setText(content, true);
      mailSender.send(message);
      log.info("Email успешно отправлен на адрес: {}", email);
    } catch (Exception e) {
      log.error("Ошибка при отправке email на адрес: {}", email, e);
    }
  }

  private String generateEmailContent(String email, String token) {
    var context = new Context();
    context.setVariable("email", email);
    context.setVariable("token", token);
    context.setVariable("confirmationLink", getConfirmationLink(token));
    return templateEngine.process("registration-confirmation.html", context);
  }

  private String getConfirmationLink(String token) {
    var httpUrl = authorizationServerProperties.getIssuerUrl() + authorizationServerProperties.getRegistrationConfirmationEndpoint();
    return UriComponentsBuilder.fromUriString(httpUrl, ParserType.WHAT_WG)
        .queryParam("token", token)
        .build().toUriString();
  }
}

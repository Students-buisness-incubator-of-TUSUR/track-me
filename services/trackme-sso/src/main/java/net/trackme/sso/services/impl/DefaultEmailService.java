package net.trackme.sso.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.sso.services.EmailService;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultEmailService implements EmailService {

  private final JavaMailSender mailSender;

  private final TemplateEngine templateEngine;

  @Override
  public void sendMail(String emailTo,
                       String emailForm,
                       String subject,
                       String templateName,
                       Map<String, Object> variables) {
    try {
      log.info("Отправка emailTo на адрес: {}", emailTo);
      var content = getContent(templateName, variables);
      var message = mailSender.createMimeMessage();
      var helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setTo(emailTo);
      helper.setFrom(emailForm);
      helper.setSubject(subject);
      helper.setText(content, true);
      mailSender.send(message);
      log.info("Email успешно отправлен на адрес: {}", emailTo);
    } catch (Exception e) {
      log.error("Ошибка при отправке emailTo на адрес: {}", emailTo, e);
    }
  }

  private String getContent(String templateName, Map<String, Object> variables) {
    var context = new Context();
    context.setVariables(variables);
    return templateEngine.process(templateName, context);
  }
}

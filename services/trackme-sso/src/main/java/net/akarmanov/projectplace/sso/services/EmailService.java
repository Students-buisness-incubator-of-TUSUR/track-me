package net.akarmanov.projectplace.sso.services;

import java.util.Map;

public interface EmailService {

  void sendMail(String emailTo,
                String emailForm,
                String subject,
                String templateName,
                Map<String, Object> variables);
}

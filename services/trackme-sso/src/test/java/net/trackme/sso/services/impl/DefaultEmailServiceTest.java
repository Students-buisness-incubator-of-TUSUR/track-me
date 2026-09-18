package net.trackme.sso.services.impl;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultEmailServiceTest {
    @Test
    void smtpFailureIsPropagatedForRetry() {
        var sender = mock(JavaMailSender.class);
        var message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("SMTP unavailable")).when(sender).send(message);
        var templates = new SpringTemplateEngine();
        templates.setTemplateResolver(new StringTemplateResolver());
        var service = new DefaultEmailService(sender, templates);

        assertThrows(MailSendException.class, () -> service.sendMail(
                "recipient@example.org", "sender@example.org", "Reminder",
                "<p>Meeting</p>", Map.of()));
    }

    @Test
    void successfulDeliveryPreservesRecipientAndSubject() throws Exception {
        var sender = mock(JavaMailSender.class);
        var message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        var templates = new SpringTemplateEngine();
        templates.setTemplateResolver(new StringTemplateResolver());
        var service = new DefaultEmailService(sender, templates);

        service.sendMail("recipient@example.org", "sender@example.org", "Reminder",
                "<p>Meeting</p>", Map.of());

        verify(sender).send(message);
        assertEquals("recipient@example.org", message.getAllRecipients()[0].toString());
        assertEquals("Reminder", message.getSubject());
    }
}

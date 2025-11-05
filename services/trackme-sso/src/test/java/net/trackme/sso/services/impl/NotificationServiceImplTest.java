package net.trackme.sso.services.impl;

import jakarta.mail.internet.MimeMessage;
import net.trackme.sso.AbstractIntegrationTest;
import net.trackme.sso.config.AppProperties;
import net.trackme.sso.dao.repository.UserRepository;
import net.trackme.sso.services.EmailService;
import net.trackme.sso.services.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderValidatorAutoConfiguration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ImportAutoConfiguration(exclude = {
        MailSenderAutoConfiguration.class,
        MailSenderValidatorAutoConfiguration.class
})
class NotificationServiceImplTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AppProperties appProperties;
    @Autowired
    private EmailService emailService;
    @MockitoBean
    private JavaMailSender javaMailSender;

    @Test
    void sendMeetingNotHappenedNotification_success() {
        // Arrange
        String teamCardUsername = "tracker";
        String teamCardName = "test team";
        String streamName = "test stream";
        String meetingLink = "test link";

        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository,
                appProperties,
                emailService);

        // Act
        notificationService.sendMeetingNotHappenedNotification(
                teamCardUsername,
                teamCardName,
                streamName,
                meetingLink);

        // Assert
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendMeetingNotHappenedNotification_userIsEmpty() {
        // Arrange
        String teamCardUsername = "";
        String teamCardName = "test team";
        String streamName = "test stream";
        String meetingLink = "test link";

        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository,
                appProperties,
                emailService);

        // Act & Assert
        assertThrows(NoSuchElementException.class,
                () -> notificationService.sendMeetingNotHappenedNotification(
                        teamCardUsername,
                        teamCardName,
                        streamName,
                        meetingLink));
    }

    @Test
    void sendTeamCardSummary_success() {
        // Arrange
        LinkedHashMap<String, String> teamCardSummaryEvent = new LinkedHashMap<>();

        List<LinkedHashMap<String, String>> teamCardSummaryEvents =
                new ArrayList<>(){{
                    add(teamCardSummaryEvent);
        }};

        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository,
                appProperties,
                emailService);

        // Act
        notificationService.sendTeamCardSummary(teamCardSummaryEvents);

        // Assert
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendTeamCardLowGradeSummary_success() {
        // Arrange
        LinkedHashMap<String, String> teamCardLowGradeSummaryEvent = new LinkedHashMap<>();

        List<LinkedHashMap<String, String>> teamCardLowGradeSummaryEvents =
                new ArrayList<>(){{
                    add(teamCardLowGradeSummaryEvent);
                }};

        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository,
                appProperties,
                emailService);

        // Act
        notificationService.sendTeamCardLowGradeSummary(teamCardLowGradeSummaryEvents);

        // Assert
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }
}
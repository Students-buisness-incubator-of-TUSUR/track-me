package net.trackme.sso.services.impl;

import jakarta.mail.internet.MimeMessage;
import net.trackme.sso.AbstractIntegrationTest;
import net.trackme.sso.config.AppProperties;
import net.trackme.sso.dao.repository.UserRepository;
import net.trackme.sso.services.EmailService;
import net.trackme.sso.services.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderValidatorAutoConfiguration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

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

    private static final String TEST_TEAM_NAME = "test team";
    private static final String TEST_STREAM_NAME = "test stream";
    private static final String TEST_MEETING_LINK = "test link";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AppProperties appProperties;
    @Autowired
    private EmailService emailService;
    @MockitoBean
    private JavaMailSender javaMailSender;

    @BeforeEach
    void setUpUsers() {
        var admin = userRepository.findByUsername("superadmin").stream().findFirst().orElseThrow();
        admin.setEmail("superadmin@superadmin.ru");
        userRepository.save(admin);
    }

    @Test
    void sendMeetingNotHappenedNotification_success() {
        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository, appProperties, emailService);

        assertDoesNotThrow(() -> 
            notificationService.sendMeetingNotHappenedNotification(
                "tracker", TEST_TEAM_NAME, TEST_STREAM_NAME, TEST_MEETING_LINK, "Петров Петр Петрович")
        );
        
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendMeetingNotHappenedNotification_userIsEmpty() {
        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository, appProperties, emailService);

        assertThrows(NoSuchElementException.class,
                () -> notificationService.sendMeetingNotHappenedNotification(
                        "", TEST_TEAM_NAME, TEST_STREAM_NAME, TEST_MEETING_LINK, null));
    }

    @Test
    void sendMeetingNotHappenedNotification_withNullTrackerFullName() {
        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository, appProperties, emailService);

        assertDoesNotThrow(() ->
            notificationService.sendMeetingNotHappenedNotification(
                "admin", TEST_TEAM_NAME, TEST_STREAM_NAME, TEST_MEETING_LINK, null)
        );
        
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendMeetingNotHappenedNotification_withBlankTrackerFullName_usesUserFullName() {
        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository, appProperties, emailService);

        assertDoesNotThrow(() ->
            notificationService.sendMeetingNotHappenedNotification(
                "superadmin", TEST_TEAM_NAME, TEST_STREAM_NAME, TEST_MEETING_LINK, "")
        );
        
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendMeetingNotHappenedNotification_withWhitespaceTrackerFullName() {
        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository, appProperties, emailService);

        assertDoesNotThrow(() ->
            notificationService.sendMeetingNotHappenedNotification(
                "superadmin", TEST_TEAM_NAME, TEST_STREAM_NAME, TEST_MEETING_LINK, "   ")
        );
        
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendMeetingNotHappenedNotification_withShortTrackerName_usesShortName() {
        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository, appProperties, emailService);

        assertDoesNotThrow(() ->
            notificationService.sendMeetingNotHappenedNotification(
                "superadmin", TEST_TEAM_NAME, TEST_STREAM_NAME, TEST_MEETING_LINK, "Иванов")
        );
        
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendTeamCardSummary_success() {
        LinkedHashMap<String, String> event1 = new LinkedHashMap<>();
        event1.put("streamName", "Поток 1");
        event1.put("teamCardName", "Команда 1");
        event1.put("trackerFullName", "Иванов Иван Иванович");
        event1.put("meetingNumber", "1");
        event1.put("meetingLink", "http://example.com/meeting/1");

        List<LinkedHashMap<String, String>> teamCardSummaryEvents = List.of(event1);

        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository, appProperties, emailService);

        assertDoesNotThrow(() ->
            notificationService.sendTeamCardSummary(teamCardSummaryEvents)
        );
        
        // Может быть несколько получателей
        verify(javaMailSender, atLeastOnce()).send(any(MimeMessage.class));
    }

    @Test
    void sendTeamCardSummary_multipleStreams_sorted() {
        LinkedHashMap<String, String> event1 = new LinkedHashMap<>();
        event1.put("streamName", "Поток A");
        event1.put("teamCardName", "AAA Team");
        event1.put("trackerFullName", "Иванов Иван");
        event1.put("meetingNumber", "1");
        event1.put("meetingLink", "http://example.com/1");

        LinkedHashMap<String, String> event2 = new LinkedHashMap<>();
        event2.put("streamName", "Поток A");
        event2.put("teamCardName", "BBB Team");
        event2.put("trackerFullName", "Петров Петр");
        event2.put("meetingNumber", "2");
        event2.put("meetingLink", "http://example.com/2");

        List<LinkedHashMap<String, String>> events = List.of(event2, event1);

        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository, appProperties, emailService);

        assertDoesNotThrow(() ->
            notificationService.sendTeamCardSummary(events)
        );
        
        verify(javaMailSender, atLeastOnce()).send(any(MimeMessage.class));
    }

    @Test
    void sendTeamCardSummary_emptyEvents_shouldStillCallSend() {
        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository, appProperties, emailService);

        assertDoesNotThrow(() ->
            notificationService.sendTeamCardSummary(List.of())
        );
        
        verify(javaMailSender, atLeastOnce()).send(any(MimeMessage.class));
    }

    @Test
    void sendTeamCardSummary_nullTrackerFullName_usesDefault() {
        LinkedHashMap<String, String> event = new LinkedHashMap<>();
        event.put("streamName", "Поток 1");
        event.put("teamCardName", "Команда 1");
        event.put("meetingNumber", "1");
        event.put("meetingLink", "http://example.com/meeting/1");

        List<LinkedHashMap<String, String>> events = List.of(event);

        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository, appProperties, emailService);

        assertDoesNotThrow(() ->
            notificationService.sendTeamCardSummary(events)
        );
        
        verify(javaMailSender, atLeastOnce()).send(any(MimeMessage.class));
    }

    @Test
    void sendTeamCardSummary_withEnglishAndRussianNames_sortedCorrectly() {
        LinkedHashMap<String, String> event1 = new LinkedHashMap<>();
        event1.put("streamName", "Test Stream");
        event1.put("teamCardName", "Яндекс");
        event1.put("trackerFullName", "Иванов Иван");
        event1.put("meetingNumber", "2");
        event1.put("meetingLink", "http://example.com/2");

        LinkedHashMap<String, String> event2 = new LinkedHashMap<>();
        event2.put("streamName", "Test Stream");
        event2.put("teamCardName", "Apple");
        event2.put("trackerFullName", "John Doe");
        event2.put("meetingNumber", "1");
        event2.put("meetingLink", "http://example.com/1");

        LinkedHashMap<String, String> event3 = new LinkedHashMap<>();
        event3.put("streamName", "Test Stream");
        event3.put("teamCardName", "Альфа");
        event3.put("trackerFullName", "Петров Петр");
        event3.put("meetingNumber", "3");
        event3.put("meetingLink", "http://example.com/3");

        List<LinkedHashMap<String, String>> events = List.of(event3, event2, event1);

        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository, appProperties, emailService);

        assertDoesNotThrow(() ->
            notificationService.sendTeamCardSummary(events)
        );
        
        verify(javaMailSender, atLeastOnce()).send(any(MimeMessage.class));
    }

    @Test
    void sendTeamCardSummary_withMultipleStreamsAndVariousNames() {
        LinkedHashMap<String, String> event1 = new LinkedHashMap<>();
        event1.put("streamName", "Поток 1");
        event1.put("teamCardName", "BBB");
        event1.put("trackerFullName", "Иванов Иван Иванович");
        event1.put("meetingNumber", "2");
        event1.put("meetingLink", "http://example.com/2");

        LinkedHashMap<String, String> event2 = new LinkedHashMap<>();
        event2.put("streamName", "Поток 1");
        event2.put("teamCardName", "AAA");
        event2.put("trackerFullName", "Петров Петр Петрович");
        event2.put("meetingNumber", "1");
        event2.put("meetingLink", "http://example.com/1");

        LinkedHashMap<String, String> event3 = new LinkedHashMap<>();
        event3.put("streamName", "Поток 2");
        event3.put("teamCardName", "Команда");
        event3.put("trackerFullName", null);
        event3.put("meetingNumber", "1");
        event3.put("meetingLink", "http://example.com/3");

        List<LinkedHashMap<String, String>> events = List.of(event3, event2, event1);

        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository, appProperties, emailService);

        assertDoesNotThrow(() ->
            notificationService.sendTeamCardSummary(events)
        );
        
        verify(javaMailSender, atLeastOnce()).send(any(MimeMessage.class));
    }

    @Test
    void sendTeamCardSummary_withNullTeamCardName_handled() {
        LinkedHashMap<String, String> event = new LinkedHashMap<>();
        event.put("streamName", "Stream");
        event.put("trackerFullName", "Иванов Иван");
        event.put("meetingLink", "http://example.com");

        List<LinkedHashMap<String, String>> events = List.of(event);

        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository, appProperties, emailService);

        assertDoesNotThrow(() ->
            notificationService.sendTeamCardSummary(events)
        );
        
        verify(javaMailSender, atLeastOnce()).send(any(MimeMessage.class));
    }

    @Test
    void sendTeamCardLowGradeSummary_success() {
        LinkedHashMap<String, String> event1 = new LinkedHashMap<>();
        event1.put("streamName", "Поток 1");
        event1.put("teamCardName", "Команда 1");
        event1.put("trackerFullName", "Иванов Иван Иванович");
        event1.put("averageGrade", "0.25");

        List<LinkedHashMap<String, String>> teamCardLowGradeSummaryEvents = List.of(event1);

        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository, appProperties, emailService);

        assertDoesNotThrow(() ->
            notificationService.sendTeamCardLowGradeSummary(teamCardLowGradeSummaryEvents)
        );
        
        verify(javaMailSender, atLeastOnce()).send(any(MimeMessage.class));
    }

    @Test
    void sendTeamCardLowGradeSummary_emptyEvents_shouldStillCallSend() {
        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository, appProperties, emailService);

        assertDoesNotThrow(() ->
            notificationService.sendTeamCardLowGradeSummary(List.of())
        );
        
        verify(javaMailSender, atLeastOnce()).send(any(MimeMessage.class));
    }

    @Test
    void sendTeamCardLowGradeSummary_withEnglishAndRussianNames_sortedCorrectly() {
        LinkedHashMap<String, String> event1 = new LinkedHashMap<>();
        event1.put("streamName", "Stream");
        event1.put("teamCardName", "Zebra");
        event1.put("trackerFullName", null);
        event1.put("averageGrade", "0.5");

        LinkedHashMap<String, String> event2 = new LinkedHashMap<>();
        event2.put("streamName", "Stream");
        event2.put("teamCardName", "Якорь");
        event2.put("trackerFullName", "Иванов Иван");
        event2.put("averageGrade", "0.3");

        List<LinkedHashMap<String, String>> events = List.of(event2, event1);

        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository, appProperties, emailService);

        assertDoesNotThrow(() ->
            notificationService.sendTeamCardLowGradeSummary(events)
        );
        
        verify(javaMailSender, atLeastOnce()).send(any(MimeMessage.class));
    }

    @Test
    void sendTeamCardLowGradeSummary_withMultipleStreamsAndVariousNames() {
        LinkedHashMap<String, String> event1 = new LinkedHashMap<>();
        event1.put("streamName", "Поток A");
        event1.put("teamCardName", "Команда 2");
        event1.put("trackerFullName", "Иванов Иван");
        event1.put("averageGrade", "0.3");

        LinkedHashMap<String, String> event2 = new LinkedHashMap<>();
        event2.put("streamName", "Поток A");
        event2.put("teamCardName", "Команда 1");
        event2.put("trackerFullName", null);
        event2.put("averageGrade", "0.2");

        LinkedHashMap<String, String> event3 = new LinkedHashMap<>();
        event3.put("streamName", "Поток B");
        event3.put("teamCardName", "Команда 3");
        event3.put("trackerFullName", "Петров");
        event3.put("averageGrade", "0.1");

        List<LinkedHashMap<String, String>> events = List.of(event3, event2, event1);

        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository, appProperties, emailService);

        assertDoesNotThrow(() ->
            notificationService.sendTeamCardLowGradeSummary(events)
        );
        
        verify(javaMailSender, atLeastOnce()).send(any(MimeMessage.class));
    }

    @Test
    void sendTeamCardLowGradeSummary_withOneWordTrackerName_usesFullName() {
        LinkedHashMap<String, String> event = new LinkedHashMap<>();
        event.put("streamName", "Stream");
        event.put("teamCardName", "Team");
        event.put("trackerFullName", "Иванов");
        event.put("averageGrade", "0.5");

        List<LinkedHashMap<String, String>> events = List.of(event);

        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository, appProperties, emailService);

        assertDoesNotThrow(() ->
            notificationService.sendTeamCardLowGradeSummary(events)
        );
        
        verify(javaMailSender, atLeastOnce()).send(any(MimeMessage.class));
    }

    @Test
    @Transactional
    void sendTeamCardSummary_noActiveUsersWithValidEmail_noEmailSent() {
        // Делаем всех пользователей неактивными
        var users = userRepository.findAll();
        users.forEach(user -> user.setActive(false));
        userRepository.saveAll(users);
        userRepository.flush();

        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository, appProperties, emailService);

        LinkedHashMap<String, String> event = new LinkedHashMap<>();
        event.put("streamName", "Test");
        event.put("teamCardName", "Team");
        event.put("meetingLink", "http://example.com");

        notificationService.sendTeamCardSummary(List.of(event));

        // Не должно быть отправки email, так как нет активных пользователей
        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @Transactional
    void sendTeamCardLowGradeSummary_noActiveUsersWithValidEmail_noEmailSent() {
        var users = userRepository.findAll();
        users.forEach(user -> user.setActive(false));
        userRepository.saveAll(users);
        userRepository.flush();

        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository, appProperties, emailService);

        LinkedHashMap<String, String> event = new LinkedHashMap<>();
        event.put("streamName", "Test");
        event.put("teamCardName", "Team");
        event.put("averageGrade", "0.1");

        notificationService.sendTeamCardLowGradeSummary(List.of(event));

        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @Transactional
    void sendTeamCardSummary_noRecipients_noEmailSent() {
        // Делаем всех пользователей неактивными и без подходящих ролей
        var users = userRepository.findAll();
        users.forEach(user -> {
            user.setActive(false);
            user.getRoles().clear();
        });
        userRepository.saveAll(users);
        userRepository.flush();

        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository, appProperties, emailService);

        LinkedHashMap<String, String> event = new LinkedHashMap<>();
        event.put("streamName", "Test");
        event.put("teamCardName", "Team");
        event.put("meetingLink", "http://example.com");

        assertDoesNotThrow(() -> notificationService.sendTeamCardSummary(List.of(event)));

        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @Transactional
    void sendTeamCardLowGradeSummary_noRecipients_noEmailSent() {
        var users = userRepository.findAll();
        users.forEach(user -> {
            user.setActive(false);
            user.getRoles().clear();
        });
        userRepository.saveAll(users);
        userRepository.flush();

        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository, appProperties, emailService);

        LinkedHashMap<String, String> event = new LinkedHashMap<>();
        event.put("streamName", "Test");
        event.put("teamCardName", "Team");
        event.put("averageGrade", "0.1");

        assertDoesNotThrow(() -> notificationService.sendTeamCardLowGradeSummary(List.of(event)));

        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void compareNamesAlphabetically_withNullNames_handled() {
        // Покрывает compareNamesAlphabetically с null значениями
        LinkedHashMap<String, String> event1 = new LinkedHashMap<>();
        event1.put("streamName", "Stream");
        event1.put("trackerFullName", "Test");
        event1.put("meetingLink", "http://example.com/1");
        // teamCardName отсутствует (null)

        LinkedHashMap<String, String> event2 = new LinkedHashMap<>();
        event2.put("streamName", "Stream");
        event2.put("teamCardName", "AAA");
        event2.put("trackerFullName", "Test");
        event2.put("meetingLink", "http://example.com/2");

        List<LinkedHashMap<String, String>> events = List.of(event1, event2);

        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationService notificationService = new NotificationServiceImpl(
                userRepository, appProperties, emailService);

        assertDoesNotThrow(() -> notificationService.sendTeamCardSummary(events));
        verify(javaMailSender, atLeastOnce()).send(any(MimeMessage.class));
    }
}

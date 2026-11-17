package net.trackme.sso.services.impl;

import net.trackme.sso.config.AppProperties;
import net.trackme.sso.dao.entity.OrganizationEntity;
import net.trackme.sso.dao.entity.RoleEntity;
import net.trackme.sso.dao.entity.UserEntity;
import net.trackme.sso.dao.repository.UserRepository;
import net.trackme.sso.services.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Проверяет, что сводные уведомления не раскрывают данные между организациями.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplOrganizationTest {

    private static final String FROM = "noreply@trackme.test";
    private static final String APP_NAME = "TrackMe";
    private static final String FIRST_EMAIL = "first-admin@trackme.test";
    private static final String SECOND_EMAIL = "second-admin@trackme.test";
    private static final String SUMMARY_TEMPLATE = "email-not-happened-meetings-summary.html";
    private static final String LOW_GRADE_TEMPLATE = "email-team-card-low-grade-summary.html";

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    private AppProperties appProperties;

    private OrganizationEntity firstOrganization;

    private OrganizationEntity secondOrganization;

    private UserEntity firstAdmin;

    private UserEntity secondAdmin;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getMail().setFrom(FROM);
        appProperties.getMail().setSubject(APP_NAME);
        appProperties.getMail().setSummarySendRoles(List.of("ADMIN", "SUPER_ADMIN"));

        firstOrganization = organization("First organization");
        secondOrganization = organization("Second organization");

        firstAdmin = activeAdmin("first-admin", FIRST_EMAIL, firstOrganization);
        secondAdmin = activeAdmin("second-admin", SECOND_EMAIL, secondOrganization);
    }

    @Test
    void sendTeamCardSummary_sendsOnlyToOrganizationRecipients() {
        when(userRepository.findAll()).thenReturn(List.of(firstAdmin, secondAdmin));
        var event = summaryEvent(firstOrganization.getId().toString());

        notificationService().sendTeamCardSummary(List.of(event));

        verify(emailService).sendMail(
                eq(FIRST_EMAIL), eq(FROM), anyString(), eq(SUMMARY_TEMPLATE), anyMap());
        verify(emailService, never()).sendMail(
                eq(SECOND_EMAIL), anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void sendTeamCardSummary_withoutOrganization_sendsToAllRecipients() {
        when(userRepository.findAll()).thenReturn(List.of(firstAdmin, secondAdmin));
        var event = summaryEvent(null);

        notificationService().sendTeamCardSummary(List.of(event));

        verify(emailService).sendMail(
                eq(FIRST_EMAIL), eq(FROM), anyString(), eq(SUMMARY_TEMPLATE), anyMap());
        verify(emailService).sendMail(
                eq(SECOND_EMAIL), eq(FROM), anyString(), eq(SUMMARY_TEMPLATE), anyMap());
    }

    @Test
    void sendTeamCardLowGradeSummary_sendsOnlyToOrganizationRecipients() {
        when(userRepository.findAll()).thenReturn(List.of(firstAdmin, secondAdmin));
        var event = lowGradeEvent(secondOrganization.getId().toString());

        notificationService().sendTeamCardLowGradeSummary(List.of(event));

        verify(emailService).sendMail(
                eq(SECOND_EMAIL), eq(FROM), anyString(), eq(LOW_GRADE_TEMPLATE), anyMap());
        verify(emailService, never()).sendMail(
                eq(FIRST_EMAIL), anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void sendTeamCardLowGradeSummary_withoutOrganization_sendsToAllRecipients() {
        when(userRepository.findAll()).thenReturn(List.of(firstAdmin, secondAdmin));
        var event = lowGradeEvent(null);

        notificationService().sendTeamCardLowGradeSummary(List.of(event));

        verify(emailService).sendMail(
                eq(FIRST_EMAIL), eq(FROM), anyString(), eq(LOW_GRADE_TEMPLATE), anyMap());
        verify(emailService).sendMail(
                eq(SECOND_EMAIL), eq(FROM), anyString(), eq(LOW_GRADE_TEMPLATE), anyMap());
    }

    private NotificationServiceImpl notificationService() {
        return new NotificationServiceImpl(userRepository, appProperties, emailService);
    }

    private OrganizationEntity organization(String name) {
        var organization = new OrganizationEntity();
        organization.setId(UUID.randomUUID());
        organization.setName(name);
        return organization;
    }

    private UserEntity activeAdmin(String username, String email,
                                   OrganizationEntity organization) {
        var role = new RoleEntity();
        role.setCode("ADMIN");

        var user = new UserEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName("Admin " + username);
        user.setActive(true);
        user.setOrganization(organization);
        user.setRoles(new HashSet<>(Set.of(role)));
        return user;
    }

    private Map<String, String> summaryEvent(String organizationId) {
        var event = new LinkedHashMap<String, String>();
        event.put("streamName", "Stream");
        event.put("teamCardName", "Team");
        event.put("trackerFullName", "Иванов Иван");
        event.put("meetingNumber", "1");
        event.put("meetingLink", "http://example.com/meeting/1");
        if (organizationId != null) {
            event.put("organizationId", organizationId);
        }
        return event;
    }

    private Map<String, String> lowGradeEvent(String organizationId) {
        var event = new LinkedHashMap<String, String>();
        event.put("streamName", "Stream");
        event.put("teamCardName", "Team");
        event.put("trackerFullName", "Иванов Иван");
        event.put("averageGrade", "0.1");
        if (organizationId != null) {
            event.put("organizationId", organizationId);
        }
        return event;
    }
}

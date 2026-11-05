package net.trackme.sso.services.impl;

import net.trackme.sso.config.AppProperties;
import net.trackme.sso.dao.entity.RoleEntity;
import net.trackme.sso.dao.entity.UserEntity;
import net.trackme.sso.dao.repository.UserRepository;
import net.trackme.sso.messaging.TeamCardEventsListener;
import net.trackme.sso.services.EmailService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LowGradeSummaryRecipientsTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Captor
    private ArgumentCaptor<String> recipientsCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> variablesCaptor;

    @Test
    void combinedEvent_sendsEveryConfiguredRecipientOneSummaryWithAllTrackers() {
        var properties = new AppProperties();
        properties.getMail().setFrom("noreply@example.com");
        properties.getMail().setSubject("TrackMe");
        properties.getMail().setSummarySendRoles(List.of("ADMIN", "SUPER_ADMIN"));
        when(userRepository.findAll()).thenReturn(List.of(
                user("admin@example.com", "ADMIN", true),
                user("superadmin@example.com", "SUPER_ADMIN", true),
                user("tracker@example.com", "TRACKER", true),
                user("inactive@example.com", "ADMIN", false),
                user(" ", "ADMIN", true)));
        var listener = new TeamCardEventsListener(
                new NotificationServiceImpl(userRepository, properties, emailService));
        List<Map<String, String>> events = List.of(
                event("First team", "First stream", "First tracker"),
                event("Second team", "Second stream", "Second tracker"));

        listener.onTeamCardLowGradeSummaryEvent(new ConsumerRecord<>(
                "team-card-low-grade-summary", 0, 0, "summary", events));

        verify(emailService, times(2)).sendMail(recipientsCaptor.capture(),
                eq("noreply@example.com"),
                eq("[TrackMe] Сводка по командам с низким рейтингом"),
                eq("email-team-card-low-grade-summary.html"), variablesCaptor.capture());
        verifyNoMoreInteractions(emailService);
        assertEquals(List.of("admin@example.com", "superadmin@example.com"),
                recipientsCaptor.getAllValues());
        var summaries = variablesCaptor.getAllValues().stream()
                .map(variables -> variables.get("summary").toString()).toList();
        assertEquals(summaries.getFirst(), summaries.getLast());
        assertTrue(summaries.getFirst().contains("First team"));
        assertTrue(summaries.getFirst().contains("Second team"));
        assertTrue(summaries.getFirst().contains("First tracker"));
        assertTrue(summaries.getFirst().contains("Second tracker"));
    }

    private UserEntity user(String email, String roleCode, boolean active) {
        var role = new RoleEntity();
        role.setCode(roleCode);
        var user = new UserEntity();
        user.setEmail(email);
        user.setFullName("Summary recipient");
        user.setRoles(Set.of(role));
        user.setActive(active);
        return user;
    }

    private Map<String, String> event(String team, String stream, String tracker) {
        return Map.of("teamCardName", team, "streamName", stream,
                "trackerFullName", tracker, "averageGrade", "0.25");
    }
}

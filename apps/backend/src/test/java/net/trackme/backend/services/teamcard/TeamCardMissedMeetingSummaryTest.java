package net.trackme.backend.services.teamcard;

import jakarta.persistence.EntityManager;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.messaging.TeamCardSummaryEvent;
import net.trackme.backend.repos.TeamCardsRepository;
import net.trackme.backend.services.exceptions.TeamCardNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamCardMissedMeetingSummaryTest {

    @Mock
    private TeamCardsService teamCardsService;

    @Mock
    private TeamCardsRepository teamCardsRepository;

    @Mock
    private TeamCardEventsProducer teamCardEventsProducer;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private TeamCardSummaryService summaryService;

    @Captor
    private ArgumentCaptor<List<TeamCardSummaryEvent>> eventsCaptor;

    @Test
    void mixedTeams_sendOneSummaryWithoutExpelledTeamsAndPreserveMeetingDetails() {
        var first = team("First team", false);
        var expelled = team("Expelled team", true);
        var second = team("Second team", false);
        givenTeams(first, expelled, second);

        summaryService.sendTeamCardsSummary(List.of(
                meeting(first, "1"), meeting(expelled, "2"),
                meeting(second, "3"), meeting(first, "4")));

        verify(teamCardEventsProducer).sendTeamCardSummaryEvent(eventsCaptor.capture());
        verifyNoMoreInteractions(teamCardEventsProducer);
        assertEquals(List.of(
                expectedEvent(first, "1"), expectedEvent(second, "3"), expectedEvent(first, "4")),
                eventsCaptor.getValue());
    }

    @Test
    void allTeamsExpelled_doesNotSendSummary() {
        var first = team("First expelled team", true);
        var second = team("Second expelled team", true);
        givenTeams(first, second);

        summaryService.sendTeamCardsSummary(List.of(meeting(first, "1"), meeting(second, "2")));

        verifyNoInteractions(teamCardEventsProducer);
    }

    @Test
    void expelledTeamWithMultipleMissedMeetings_isExcludedCompletely() {
        var expelled = team("Expelled team", true);
        givenTeams(expelled);

        summaryService.sendTeamCardsSummary(List.of(meeting(expelled, "1"), meeting(expelled, "2")));

        verifyNoInteractions(teamCardEventsProducer);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false})
    void nonExpelledTeamWithHighGrade_isStillIncluded(Boolean passive) {
        var eligible = team("Eligible team", passive);
        givenTeams(eligible);

        summaryService.sendTeamCardsSummary(List.of(meeting(eligible, "1")));

        verify(teamCardEventsProducer).sendTeamCardSummaryEvent(eventsCaptor.capture());
        assertEquals(List.of(expectedEvent(eligible, "1")), eventsCaptor.getValue());
    }

    @Test
    void restoredTeam_participatesInNextSummary() {
        var restored = team("Restored team", true);
        givenTeams(restored);
        var meetings = List.of(meeting(restored, "1"));

        summaryService.sendTeamCardsSummary(meetings);
        verifyNoInteractions(teamCardEventsProducer);

        restored.setPassive(false);
        summaryService.sendTeamCardsSummary(meetings);

        verify(teamCardEventsProducer).sendTeamCardSummaryEvent(eventsCaptor.capture());
        assertEquals(List.of(expectedEvent(restored, "1")), eventsCaptor.getValue());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void teamWithoutActiveStream_remainsExcluded(boolean hasEndedStream) {
        var eligible = team("Without active stream", false);
        eligible.setStreams(hasEndedStream
                ? Set.of(Stream.builder().endDate(LocalDate.now(ZoneId.systemDefault()).minusDays(1)).build())
                : Set.of());
        givenTeams(eligible);

        summaryService.sendTeamCardsSummary(List.of(meeting(eligible, "1")));

        verifyNoInteractions(teamCardEventsProducer);
    }

    @Test
    void missingTeam_doesNotPreventOtherTeamsFromBeingReported() {
        var missing = team("Deleted team", false);
        var eligible = team("Eligible team", false);
        when(teamCardsService.getTeamCard(missing.getId()))
                .thenThrow(new TeamCardNotFoundException(missing.getId()));
        givenTeams(eligible);

        summaryService.sendTeamCardsSummary(List.of(meeting(missing, "1"), meeting(eligible, "2")));

        verify(teamCardEventsProducer).sendTeamCardSummaryEvent(eventsCaptor.capture());
        assertEquals(List.of(expectedEvent(eligible, "2")), eventsCaptor.getValue());
    }

    @Test
    void emptyInput_doesNotLoadTeamsOrSendSummary() {
        summaryService.sendTeamCardsSummary(List.of());

        verifyNoInteractions(teamCardsService, teamCardEventsProducer);
    }

    @Test
    void missingTrackerName_keepsExistingFallback() {
        var eligible = team("Eligible team", false);
        givenTeams(eligible);
        var event = new HashMap<>(meeting(eligible, "1"));
        event.remove("trackerFullName");

        summaryService.sendTeamCardsSummary(List.of(event));

        verify(teamCardEventsProducer).sendTeamCardSummaryEvent(eventsCaptor.capture());
        assertEquals("Не назначен", eventsCaptor.getValue().getFirst().trackerFullName());
    }

    private void givenTeams(TeamCard... teams) {
        for (var team : teams) {
            when(teamCardsService.getTeamCard(team.getId())).thenReturn(team);
        }
    }

    private TeamCard team(String name, Boolean passive) {
        var today = LocalDate.now(ZoneId.systemDefault());
        var stream = Stream.builder()
                .name("Active stream")
                .startDate(today.minusDays(1))
                .endDate(today.plusDays(1))
                .build();
        return TeamCard.builder()
                .id(UUID.randomUUID())
                .name(name)
                .passive(passive)
                .averageGrade(BigDecimal.ONE)
                .streams(Set.of(stream))
                .build();
    }

    private Map<String, String> meeting(TeamCard team, String number) {
        return Map.of(
                "teamCardId", team.getId().toString(),
                "meetingNumber", number,
                "meetingLink", "https://example.test/meetings/" + number,
                "trackerFullName", "Tracker of " + team.getName());
    }

    private TeamCardSummaryEvent expectedEvent(TeamCard team, String number) {
        return new TeamCardSummaryEvent(team.getName(), "Active stream", number,
                "https://example.test/meetings/" + number, "Tracker of " + team.getName());
    }
}

package net.trackme.backend.services.teamcard;

import net.trackme.backend.domain.Stream;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.messaging.TeamCardLowGradeSummaryEvent;
import net.trackme.backend.repos.TeamCardsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamCardLowGradeSummaryTest {

    @Mock
    private TeamCardsService teamCardsService;

    @Mock
    private TeamCardsRepository teamCardsRepository;

    @Mock
    private TeamCardEventsProducer teamCardEventsProducer;

    @InjectMocks
    private TeamCardSummaryService summaryService;

    @Captor
    private ArgumentCaptor<List<TeamCardLowGradeSummaryEvent>> eventsCaptor;

    @Test
    void mixedTeams_includeOnlyNonExpelledTeamsWithLowGrades() {
        when(teamCardsRepository.findAll()).thenReturn(List.of(
                team("Low grade", "0", false),
                team("At threshold", "0.25", false),
                team("Expelled", "0.1", true),
                team("Above threshold", "0.26", false)));

        summaryService.reportAboutTeamCardLowGrades();

        verify(teamCardEventsProducer).sendTeamCardLowGradeSummaryEvent(eventsCaptor.capture());
        assertEquals(List.of(
                new TeamCardLowGradeSummaryEvent(
                        "Low grade", "Active stream", BigDecimal.ZERO, "Test tracker"),
                new TeamCardLowGradeSummaryEvent(
                        "At threshold", "Active stream", new BigDecimal("0.25"), "Test tracker")),
                eventsCaptor.getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.25"})
    void expelledTeam_isExcludedRegardlessOfLowGrade(String grade) {
        when(teamCardsRepository.findAll()).thenReturn(List.of(team("Expelled", grade, true)));

        summaryService.reportAboutTeamCardLowGrades();

        verifyNoInteractions(teamCardEventsProducer);
    }

    @Test
    void allLowGradeTeamsExpelled_doesNotSendSummary() {
        when(teamCardsRepository.findAll()).thenReturn(List.of(
                team("Expelled zero", "0", true),
                team("Expelled at threshold", "0.25", true),
                team("Above threshold", "0.5", false)));

        summaryService.reportAboutTeamCardLowGrades();

        verifyNoInteractions(teamCardEventsProducer);
    }

    @Test
    void gradeAtThreshold_isIncludedButAboveThresholdIsExcluded() {
        when(teamCardsRepository.findAll()).thenReturn(List.of(
                team("At threshold", "0.25", false),
                team("Above threshold", "0.2501", false)));

        summaryService.reportAboutTeamCardLowGrades();

        verify(teamCardEventsProducer).sendTeamCardLowGradeSummaryEvent(eventsCaptor.capture());
        assertEquals(List.of("At threshold"), eventTeamNames());
    }

    @Test
    void restoredTeam_participatesInNextSummary() {
        var restoredTeam = team("Restored", "0.1", true);
        when(teamCardsRepository.findAll()).thenReturn(List.of(restoredTeam));

        summaryService.reportAboutTeamCardLowGrades();
        verifyNoInteractions(teamCardEventsProducer);

        restoredTeam.setPassive(false);
        summaryService.reportAboutTeamCardLowGrades();

        verify(teamCardEventsProducer).sendTeamCardLowGradeSummaryEvent(eventsCaptor.capture());
        assertEquals(List.of("Restored"), eventTeamNames());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void teamWithoutActiveStream_isExcluded(boolean hasInactiveStream) {
        var teamWithoutActiveStream = team("Without active stream", "0", false);
        teamWithoutActiveStream.setStreams(hasInactiveStream
                ? Set.of(Stream.builder()
                        .name("Ended stream")
                        .endDate(LocalDate.now(ZoneId.systemDefault()).minusDays(1))
                        .build())
                : Set.of());
        when(teamCardsRepository.findAll()).thenReturn(List.of(teamWithoutActiveStream));

        summaryService.reportAboutTeamCardLowGrades();

        verifyNoInteractions(teamCardEventsProducer);
    }

    @Test
    void nullPassive_isNotConsideredExpelled() {
        when(teamCardsRepository.findAll()).thenReturn(List.of(team("Legacy team", "0.1", null)));

        summaryService.reportAboutTeamCardLowGrades();

        verify(teamCardEventsProducer).sendTeamCardLowGradeSummaryEvent(eventsCaptor.capture());
        assertEquals(List.of("Legacy team"), eventTeamNames());
    }

    private TeamCard team(String name, String grade, Boolean passive) {
        return TeamCard.builder()
                .name(name)
                .averageGrade(new BigDecimal(grade))
                .passive(passive)
                .trackerFullName("Test tracker")
                .streams(Set.of(Stream.builder().name("Active stream").build()))
                .build();
    }

    private List<String> eventTeamNames() {
        return eventsCaptor.getValue().stream()
                .map(TeamCardLowGradeSummaryEvent::teamCardName)
                .toList();
    }
}

package net.trackme.backend.services.teamcard;

import jakarta.persistence.EntityManager;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamCardLowGradeSummaryTest {

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
    private ArgumentCaptor<List<TeamCardLowGradeSummaryEvent>> eventsCaptor;

    @Test
    void mixedTeams_includeOnlyNonExpelledTeamsWithLowGrades() {
        givenTeams(List.of(
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
        givenTeams(List.of(team("Expelled", grade, true)));

        summaryService.reportAboutTeamCardLowGrades();

        verifyNoInteractions(teamCardEventsProducer);
    }

    @Test
    void allLowGradeTeamsExpelled_doesNotSendSummary() {
        givenTeams(List.of(
                team("Expelled zero", "0", true),
                team("Expelled at threshold", "0.25", true),
                team("Above threshold", "0.5", false)));

        summaryService.reportAboutTeamCardLowGrades();

        verifyNoInteractions(teamCardEventsProducer);
    }

    @Test
    void gradeAtThreshold_isIncludedButAboveThresholdIsExcluded() {
        givenTeams(List.of(
                team("At threshold", "0.25", false),
                team("Above threshold", "0.2501", false)));

        summaryService.reportAboutTeamCardLowGrades();

        verify(teamCardEventsProducer).sendTeamCardLowGradeSummaryEvent(eventsCaptor.capture());
        assertEquals(List.of("At threshold"), eventTeamNames());
    }

    @Test
    void restoredTeam_participatesInNextSummary() {
        var restoredTeam = team("Restored", "0.1", true);
        givenTeams(List.of(restoredTeam));

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
        givenTeams(List.of(teamWithoutActiveStream));

        summaryService.reportAboutTeamCardLowGrades();

        verifyNoInteractions(teamCardEventsProducer);
    }

    @Test
    void nullPassive_isNotConsideredExpelled() {
        givenTeams(List.of(team("Legacy team", "0.1", null)));

        summaryService.reportAboutTeamCardLowGrades();

        verify(teamCardEventsProducer).sendTeamCardLowGradeSummaryEvent(eventsCaptor.capture());
        assertEquals(List.of("Legacy team"), eventTeamNames());
    }

    @Test
    void multiplePages_sendEachBatchBeforeLoadingNextPage() {
        var teams = teams(21, false);
        var firstPage = PageRequest.of(0, 10, Sort.by("id"));
        givenPage(firstPage, teams.subList(0, 10), true);
        givenPage(firstPage.next(), teams.subList(10, 20), true);
        givenPage(firstPage.next().next(), teams.subList(20, 21), false);

        summaryService.reportAboutTeamCardLowGrades();

        var order = inOrder(teamCardsRepository, teamCardEventsProducer, entityManager);
        for (int page = 0; page < 3; page++) {
            order.verify(teamCardsRepository).findAllBy(firstPage.withPage(page));
            order.verify(teamCardEventsProducer)
                    .sendTeamCardLowGradeSummaryEvent(eventsCaptor.capture());
            order.verify(entityManager).clear();
        }
        verifyNoMoreInteractions(teamCardsRepository, teamCardEventsProducer, entityManager);
        assertEquals(List.of(10, 10, 1), eventsCaptor.getAllValues().stream()
                .map(List::size).toList());
        assertEquals(teams.stream().map(TeamCard::getName).toList(),
                eventsCaptor.getAllValues().stream().flatMap(List::stream)
                        .map(TeamCardLowGradeSummaryEvent::teamCardName).toList());
    }

    @Test
    void fullyFilteredPage_doesNotStopFollowingPages() {
        var firstPage = PageRequest.of(0, 10, Sort.by("id"));
        givenPage(firstPage, teams(10, true), true);
        givenPage(firstPage.next(), List.of(
                team("Eligible", "0.25", false),
                team("Expelled", "0", true),
                team("Above threshold", "0.26", false)), false);

        summaryService.reportAboutTeamCardLowGrades();

        var order = inOrder(teamCardsRepository, teamCardEventsProducer, entityManager);
        order.verify(teamCardsRepository).findAllBy(firstPage);
        order.verify(entityManager).clear();
        order.verify(teamCardsRepository).findAllBy(firstPage.next());
        order.verify(teamCardEventsProducer).sendTeamCardLowGradeSummaryEvent(eventsCaptor.capture());
        order.verify(entityManager).clear();
        verifyNoMoreInteractions(teamCardsRepository, teamCardEventsProducer, entityManager);
        assertEquals(List.of("Eligible"), eventTeamNames());
    }

    @Test
    void allPagesExpelled_doesNotSendSummary() {
        var firstPage = PageRequest.of(0, 10, Sort.by("id"));
        givenPage(firstPage, teams(10, true), true);
        givenPage(firstPage.next(), teams(1, true), false);

        summaryService.reportAboutTeamCardLowGrades();

        verify(teamCardsRepository).findAllBy(firstPage);
        verify(teamCardsRepository).findAllBy(firstPage.next());
        verifyNoMoreInteractions(teamCardsRepository);
        verifyNoInteractions(teamCardEventsProducer);
    }

    @Test
    void fullLastPage_doesNotFetchAnExtraPage() {
        givenTeams(teams(10, false));

        summaryService.reportAboutTeamCardLowGrades();

        verify(teamCardsRepository).findAllBy(PageRequest.of(0, 10, Sort.by("id")));
        verifyNoMoreInteractions(teamCardsRepository);
        verify(teamCardEventsProducer).sendTeamCardLowGradeSummaryEvent(eventsCaptor.capture());
        assertEquals(10, eventsCaptor.getValue().size());
    }

    @Test
    void emptyRepository_doesNotSendSummaryOrLoadAllTeams() {
        givenTeams(List.of());

        summaryService.reportAboutTeamCardLowGrades();

        verify(teamCardsRepository).findAllBy(PageRequest.of(0, 10, Sort.by("id")));
        verify(teamCardsRepository, never()).findAll();
        verifyNoMoreInteractions(teamCardsRepository);
        verifyNoInteractions(teamCardEventsProducer);
    }

    private void givenTeams(List<TeamCard> teams) {
        givenPage(PageRequest.of(0, 10, Sort.by("id")), teams, false);
    }

    private void givenPage(PageRequest page, List<TeamCard> teams, boolean hasNext) {
        when(teamCardsRepository.findAllBy(page)).thenReturn(new SliceImpl<>(teams, page, hasNext));
    }

    private List<TeamCard> teams(int count, Boolean passive) {
        return IntStream.range(0, count)
                .mapToObj(index -> team("Team " + index, "0.1", passive))
                .toList();
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

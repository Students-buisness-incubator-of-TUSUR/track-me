package net.trackme.backend.repositories;

import net.trackme.backend.AbstractIntegrationTest;
import net.trackme.backend.domain.ReadinessLevel;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.models.TeamCardStatus;
import net.trackme.backend.repos.StreamRepository;
import net.trackme.backend.repos.TeamCardsRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;


import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class TeamCardsRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TeamCardsRepository teamCardsRepository;

    @Autowired
    private StreamRepository streamRepository;

    private Stream stream2024;
    private Stream stream2025;

    @Test
    void streamRequirementExcludesOrphansBeforePaginationWithoutDuplicatingTeams() {
        saveTeamCard("tracker1", "Orphan", 1.0);
        saveTeamCard("tracker1", "Linked", 2.0, stream2024);
        var both = TeamCard.builder().username("tracker1").name("Multiple streams").status(TeamCardStatus.OK)
            .meetingRoomLink("https://example.com").readinessLevel(ReadinessLevel.LEVEL_1)
            .streams(Set.of(stream2024, stream2025)).build();
        teamCardsRepository.saveAndFlush(both);

        var firstPage = teamCardsRepository.findAll(
            net.trackme.backend.domain.spec.TeamCardSpecification.hasStream(),
            org.springframework.data.domain.PageRequest.of(0, 1));
        var secondPage = teamCardsRepository.findAll(
            net.trackme.backend.domain.spec.TeamCardSpecification.hasStream(),
            org.springframework.data.domain.PageRequest.of(1, 1));

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(java.util.stream.Stream.concat(firstPage.stream(), secondPage.stream()).map(TeamCard::getName))
            .containsExactlyInAnyOrder("Linked", "Multiple streams");
    }

    @BeforeEach
    void setUp() {
        teamCardsRepository.deleteAll();
        streamRepository.deleteAll();

        stream2024 = streamRepository.save(Stream.builder().name("Поток 2024").build());
        stream2025 = streamRepository.save(Stream.builder().name("Поток 2025").build());
    }

    @Test
    void getAverageGradesByUser_shouldCalculateCorrectAverages() {
        // Arrange
        saveTeamCard("tracker1", "Team A", 4.0);
        saveTeamCard("tracker1", "Team B", 5.0);
        saveTeamCard("tracker2", "Team C", 3.0);

        // Act
        Map<String, Double> results = teamCardsRepository.getAverageGradesByUser(null);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get("tracker1")).isEqualTo(4.5);
        assertThat(results.get("tracker2")).isEqualTo(3.0);
    }

    @Test
    void getAverageGradesByUser_withSpecification_shouldFilterDataBeforeAggregation() {
        // Arrange
        saveTeamCard("tracker1", "Apple", 5.0);
        saveTeamCard("tracker1", "Banana", 1.0);
        saveTeamCard("tracker2", "Apple Pie", 4.0);

        Specification<TeamCard> spec = (root, query, cb) ->
                cb.like(root.get("name"), "%Apple%");

        // Act
        Map<String, Double> results = teamCardsRepository.getAverageGradesByUser(spec);

        assertThat(results.get("tracker1")).isEqualTo(5.0);
        assertThat(results.get("tracker2")).isEqualTo(4.0);
        assertThat(results).hasSize(2);
    }

    @Test
    void getAverageGradesByUser_shouldReturnEmptyMapWhenNoData() {
        // Act
        Map<String, Double> results = teamCardsRepository.getAverageGradesByUser(null);

        // Assert
        assertThat(results).isEmpty();
    }

    @Test
    void getAverageGradesByUser_shouldHandleNullGradesAsZero() {
        // Arrange
        saveTeamCard("tracker1", "No Grade Team", null);

        // Act
        Map<String, Double> results = teamCardsRepository.getAverageGradesByUser(null);

        // Assert
        assertThat(results.get("tracker1")).isEqualTo(0.0);
    }

    @Test
    void getAverageGradesByUser_withStreamFilter_shouldCalculateTrackerRatingForSpecificStream() {
        // Arrange
        saveTeamCard("ivanov", "Alpha", 5.0, stream2024);
        saveTeamCard("ivanov", "Beta", 3.0, stream2025);
        saveTeamCard("petrov", "Gamma", 4.0, stream2024);

        // Act & Assert
        Map<String, Double> allTimeGrades = teamCardsRepository.getAverageGradesByUser(null);
        assertThat(allTimeGrades.get("ivanov")).isEqualTo(4.0);
        assertThat(allTimeGrades.get("petrov")).isEqualTo(4.0);

        Specification<TeamCard> spec2024 = (root, query, cb) ->
                cb.equal(root.join("streams").get("name"), "Поток 2024");

        Map<String, Double> grades2024 = teamCardsRepository.getAverageGradesByUser(spec2024);
        assertThat(grades2024.get("ivanov")).isEqualTo(5.0);
        assertThat(grades2024.get("petrov")).isEqualTo(4.0);

        Specification<TeamCard> spec2025 = (root, query, cb) ->
                cb.equal(root.join("streams").get("name"), "Поток 2025");

        Map<String, Double> grades2025 = teamCardsRepository.getAverageGradesByUser(spec2025);
        assertThat(grades2025.get("ivanov")).isEqualTo(3.0);
        assertThat(grades2025.containsKey("petrov")).isFalse();
    }

    private void saveTeamCard(String username, String teamName, Double grade, Stream stream) {
        teamCardsRepository.save(TeamCard.builder()
                .username(username)
                .name(teamName)
                .status(TeamCardStatus.OK)
                .readinessLevel(ReadinessLevel.LEVEL_3)
                .meetingRoomLink("https://test-link.com")
                .averageGrade(grade == null ? null : BigDecimal.valueOf(grade))
                .streams(stream != null ? Set.of(stream) : Set.of())
                .enabled(true)
                .build());
    }

    private void saveTeamCard(String username, String teamName, Double grade) {
        saveTeamCard(username, teamName, grade, null);
    }
}

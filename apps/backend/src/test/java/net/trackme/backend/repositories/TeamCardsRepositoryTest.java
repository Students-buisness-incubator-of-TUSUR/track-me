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
        assertThat(results).containsEntry("tracker1", 4.5);
        assertThat(results).containsEntry("tracker2", 3.0);
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

        assertThat(results).containsEntry("tracker1", 5.0);
        assertThat(results).containsEntry("tracker2", 4.0);
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
        assertThat(results).containsEntry("tracker1", 0.0);
    }

    @Test
    void getAverageGradesByUser_withStreamFilter_shouldCalculateTrackerRatingForSpecificStream() {
        // Arrange
        saveTeamCard("ivanov", "Alpha", 5.0, stream2024);
        saveTeamCard("ivanov", "Beta", 3.0, stream2025);
        saveTeamCard("petrov", "Gamma", 4.0, stream2024);

        // Act & Assert
        Map<String, Double> allTimeGrades = teamCardsRepository.getAverageGradesByUser(null);
        assertThat(allTimeGrades).containsEntry("ivanov", 4.0);
        assertThat(allTimeGrades).containsEntry("petrov", 4.0);

        Specification<TeamCard> spec2024 = (root, query, cb) ->
                cb.equal(root.join("streams").get("name"), "Поток 2024");

        Map<String, Double> grades2024 = teamCardsRepository.getAverageGradesByUser(spec2024);
        assertThat(grades2024).containsEntry("ivanov", 5.0);
        assertThat(grades2024).containsEntry("petrov", 4.0);

        Specification<TeamCard> spec2025 = (root, query, cb) ->
                cb.equal(root.join("streams").get("name"), "Поток 2025");

        Map<String, Double> grades2025 = teamCardsRepository.getAverageGradesByUser(spec2025);
        assertThat(grades2025).containsEntry("ivanov", 3.0);
        assertThat(grades2025).doesNotContainKey("petrov");
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
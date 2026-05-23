package net.trackme.meetingservice.dao;

import net.trackme.meetingservice.entities.Meeting;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционный тест для MeetingMetadataRepository.
 * Использует Testcontainers для PostgreSQL или H2 для CI.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class MeetingMetadataRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MeetingMetadataRepository metadataRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private TestEntityManager entityManager;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("db_schema", () -> "public");
    }

    @Test
    void updatePassiveFlag_shouldUpdateAllMeetingsForTeam() {
        // Arrange
        UUID teamId = UUID.randomUUID();

        Meeting meeting1 = Meeting.builder()
                .teamCardId(teamId)
                .teamCardPassive(false)
                .startDate(OffsetDateTime.now())
                .build();

        Meeting meeting2 = Meeting.builder()
                .teamCardId(teamId)
                .teamCardPassive(false)
                .startDate(OffsetDateTime.now())
                .build();

        Meeting otherTeamMeeting = Meeting.builder()
                .teamCardId(UUID.randomUUID())
                .teamCardPassive(false)
                .startDate(OffsetDateTime.now())
                .build();

        meetingRepository.save(meeting1);
        meetingRepository.save(meeting2);
        meetingRepository.save(otherTeamMeeting);
        meetingRepository.flush();

        // Act
        metadataRepository.updatePassiveFlag(teamId, true);
        meetingRepository.flush();
        entityManager.clear();

        // Assert
        Meeting updated1 = meetingRepository.findById(meeting1.getId()).orElseThrow();
        Meeting updated2 = meetingRepository.findById(meeting2.getId()).orElseThrow();
        Meeting updatedOther = meetingRepository.findById(otherTeamMeeting.getId()).orElseThrow();

        assertTrue(updated1.getTeamCardPassive());
        assertTrue(updated2.getTeamCardPassive());
        assertFalse(updatedOther.getTeamCardPassive()); // Другая команда не должна измениться
    }

    @Test
    void updatePassiveFlag_shouldSetFalseToTrue() {
        // Arrange
        UUID teamId = UUID.randomUUID();

        Meeting meeting = Meeting.builder()
                .teamCardId(teamId)
                .teamCardPassive(false)
                .startDate(OffsetDateTime.now())
                .build();

        meetingRepository.save(meeting);
        meetingRepository.flush();

        // Act
        metadataRepository.updatePassiveFlag(teamId, true);
        meetingRepository.flush();
        entityManager.clear();

        // Assert
        Meeting updated = meetingRepository.findById(meeting.getId()).orElseThrow();
        assertTrue(updated.getTeamCardPassive());
    }

    @Test
    void updatePassiveFlag_shouldSetTrueToFalse() {
        // Arrange
        UUID teamId = UUID.randomUUID();

        Meeting meeting = Meeting.builder()
                .teamCardId(teamId)
                .teamCardPassive(true)
                .startDate(OffsetDateTime.now())
                .build();

        meetingRepository.save(meeting);
        meetingRepository.flush();

        // Act
        metadataRepository.updatePassiveFlag(teamId, false);
        meetingRepository.flush();
        entityManager.clear();

        // Assert
        Meeting updated = meetingRepository.findById(meeting.getId()).orElseThrow();
        assertFalse(updated.getTeamCardPassive());
    }

    @Test
    void updatePassiveFlag_whenNoMeetingsForTeam_shouldDoNothing() {
        // Arrange
        UUID teamId = UUID.randomUUID();

        Meeting otherMeeting = Meeting.builder()
                .teamCardId(UUID.randomUUID())
                .teamCardPassive(false)
                .startDate(OffsetDateTime.now())
                .build();

        meetingRepository.save(otherMeeting);
        meetingRepository.flush();

        // Act - не должно выбросить исключение
        assertDoesNotThrow(() -> {
            metadataRepository.updatePassiveFlag(teamId, true);
            meetingRepository.flush();
        });
    }
}
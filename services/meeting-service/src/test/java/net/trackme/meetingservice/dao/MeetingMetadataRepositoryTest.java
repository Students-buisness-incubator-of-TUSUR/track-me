package net.trackme.meetingservice.dao;

import net.trackme.meetingservice.entities.Meeting;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {"db_schema=public"})
class MeetingMetadataRepositoryTest {

    @Autowired
    private MeetingMetadataRepository repository;

    @Autowired
    private TestEntityManager entityManager;

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

        entityManager.persist(meeting1);
        entityManager.persist(meeting2);
        entityManager.flush();

        // Act
        repository.updatePassiveFlag(teamId, true);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Meeting updated1 = entityManager.find(Meeting.class, meeting1.getId());
        Meeting updated2 = entityManager.find(Meeting.class, meeting2.getId());
        assertTrue(updated1.getTeamCardPassive());
        assertTrue(updated2.getTeamCardPassive());
    }
}
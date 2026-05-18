package net.trackme.backend.services.teamcard;

import net.trackme.backend.BaseApplicationTest;
import net.trackme.backend.domain.ReadinessLevel;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.models.TeamCardStatus;
import net.trackme.backend.repos.TeamCardsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class TeamCardSummaryServiceTest extends BaseApplicationTest {

    @Autowired
    private TeamCardsRepository teamCardsRepository;

    @Autowired
    private TeamCardSummaryService teamCardSummaryService;

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    protected TeamCard teamCard;

    @BeforeEach
    void initUser() {
        stream = streamRepository.save(Stream.builder()
                .name("stream 1")
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .build());

        teamCard = teamCardsRepository.save(TeamCard.builder()
                .name("test team card")
                .enabled(true)
                .username("tracker")
                .readinessLevel(ReadinessLevel.LEVEL_2)
                .streams(Set.of(stream))
                .status(TeamCardStatus.MANY_ISSUES)
                .meetingRoomLink("meetingLink@gmail.com")
                .averageGrade(BigDecimal.ZERO)
                .build());
    }

    @AfterEach
    void deleteUser() {
        streamRepository.deleteAllInBatch();
        teamCardsRepository.deleteAllInBatch();
    }

    @Test
    void sendTeamCardsSummary_success() {
        // Arrange
        LinkedHashMap<String, String> meetingSummaryEvent =
                new LinkedHashMap<>(){{
                    put("teamCardId", String.valueOf(teamCard.getId()));
                    put("meetingNumber", "test meeting");
                    put("meetingLink", "test link");
                }};

        List<LinkedHashMap<String, String>> teamCardSummaryEvents =
                new ArrayList<>(){{
                    add(meetingSummaryEvent);
                }};

        // Act
        teamCardSummaryService.sendTeamCardsSummary(teamCardSummaryEvents);

        // Assert
        verify(kafkaTemplate).send(any(Message.class));
    }

    @Test
    void reportAboutTeamCardLowGrades_success() {
        // Act
        teamCardSummaryService.reportAboutTeamCardLowGrades();

        // Assert
        verify(kafkaTemplate).send(any(Message.class));
    }

    @Test
    void sendTeamCardsSummary_emptyList_shouldNotSend() {
        teamCardSummaryService.sendTeamCardsSummary(List.of());
        // Проверяем, что kafka-продюсер не вызывался
        verify(kafkaTemplate, never()).send(any(Message.class));
    }

    @Test
    void sendTeamCardsSummary_teamCardNotFound_shouldSkipAndContinue() {
        var event = new LinkedHashMap<String, String>();
        event.put("teamCardId", UUID.randomUUID().toString());
        event.put("meetingNumber", "1");
        event.put("meetingLink", "http://test.com");
        
        // Не падает — просто логирует предупреждение
        teamCardSummaryService.sendTeamCardsSummary(List.of(event));
        
        // Проверяем, что kafka-продюсер не вызывался (нет валидной карточки)
        verify(kafkaTemplate, never()).send(any(Message.class));
    }
}

package net.trackme.meetingservice.messaging.backend;

import net.trackme.meetingservice.dao.MeetingMetadataRepository;
import net.trackme.meetingservice.services.integration.sso.SsoApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты для проверки обработки события обновления карточки команды
 * с акцентом на вызов метода updatePassiveFlag.
 */
@ExtendWith(MockitoExtension.class)
class TeamCardEventConsumerPassiveTest {

    /**
     * Репозиторий метаданных встреч.
     */
    @Mock
    private MeetingMetadataRepository metadataRepository;

    /**
     * Клиент для взаимодействия с SSO сервисом.
     */
    @Mock
    private SsoApiClient ssoApiClient;

    /**
     * Тестируемый потребитель событий.
     */
    @InjectMocks
    private TeamCardEventConsumer teamCardEventConsumer;

    @Test
    void handleTeamCardUpdatedShouldCallUpdatePassiveFlag() {
        UUID teamId = UUID.randomUUID();
        Boolean newPassive = true;

        TeamCardUpdatedEvent event = new TeamCardUpdatedEvent(
                teamId, "Name", "user", newPassive, "FullName"
        );

        when(ssoApiClient.getTrackers()).thenReturn(Collections.emptyList());

        teamCardEventConsumer.handleTeamCardUpdated(event);

        verify(metadataRepository).updatePassiveFlag(teamId, newPassive);
    }

    @Test
    void handleTeamCardUpdatedWithNewPassiveFalseShouldCallUpdatePassiveFlag() {
        UUID teamId = UUID.randomUUID();
        Boolean newPassive = false;

        TeamCardUpdatedEvent event = new TeamCardUpdatedEvent(
                teamId, "Name", "user", newPassive, null
        );

        when(ssoApiClient.getTrackers()).thenReturn(Collections.emptyList());

        teamCardEventConsumer.handleTeamCardUpdated(event);

        verify(metadataRepository).updatePassiveFlag(teamId, newPassive);
    }

    @Test
    void handleTeamCardUpdatedWithNewPassiveNullShouldStillCallUpdatePassiveFlag() {
        UUID teamId = UUID.randomUUID();
        TeamCardUpdatedEvent event = new TeamCardUpdatedEvent(
                teamId, "Name", "user", null, null
        );

        when(ssoApiClient.getTrackers()).thenReturn(Collections.emptyList());

        teamCardEventConsumer.handleTeamCardUpdated(event);

        verify(metadataRepository).updatePassiveFlag(teamId, null);
    }
}

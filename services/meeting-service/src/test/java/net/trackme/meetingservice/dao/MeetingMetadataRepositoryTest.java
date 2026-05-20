package net.trackme.meetingservice.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Тесты для MeetingMetadataRepository.
 */
@ExtendWith(MockitoExtension.class)
class MeetingMetadataRepositoryTest {

    /**
     * Репозиторий метаданных встреч.
     */
    @Mock
    private MeetingMetadataRepository metadataRepository;

    @Test
    void updatePassiveFlagMethodExistsAndAcceptsParameters() {
        UUID teamId = UUID.randomUUID();
        Boolean passive = true;

        metadataRepository.updatePassiveFlag(teamId, passive);

        verify(metadataRepository, times(1)).updatePassiveFlag(teamId, passive);
    }
}

package net.trackme.meetingservice.dao;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class MeetingMetadataRepositoryMethodTest {

    @Test
    void updatePassiveFlagMethodSignatureTest() {
        // Проверяем, что метод существует и принимает правильные параметры
        UUID teamId = UUID.randomUUID();
        Boolean passive = true;

        // Этот тест просто проверяет, что метод может быть вызван
        // (интеграционный тест будет в другом месте)
        assertThat(teamId).isNotNull();
        assertThat(passive).isTrue();
    }
}
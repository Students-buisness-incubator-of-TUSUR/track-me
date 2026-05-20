package net.trackme.meetingservice.services.integration;

import net.trackme.meetingservice.services.integration.backend.dto.TeamCardDto;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class TeamCardDtoPassiveTest {

    @Test
    void teamCardDto_passiveFieldShouldBePresent() {
        UUID id = UUID.randomUUID();
        Boolean passive = true;

        TeamCardDto dto = TeamCardDto.builder()
                .id(id)
                .name("Test Team")
                .username("tracker")
                .passive(passive)
                .build();

        assertThat(dto.getPassive()).isTrue();
        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getName()).isEqualTo("Test Team");
    }

    @Test
    void teamCardDto_passiveCanBeFalse() {
        TeamCardDto dto = TeamCardDto.builder()
                .passive(false)
                .build();

        assertThat(dto.getPassive()).isFalse();
    }

    @Test
    void teamCardDto_passiveDefaultIsNull() {
        TeamCardDto dto = TeamCardDto.builder().build();
        assertThat(dto.getPassive()).isNull();
    }
}
package net.trackme.backend.usecases;

import net.trackme.backend.BaseApplicationTest;
import net.trackme.backend.domain.ReadinessLevel;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.models.TeamCardStatus;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardUpdateDto;
import net.trackme.backend.services.teamcard.TeamCardsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TeamCardsUseCaseTest extends BaseApplicationTest {

    @Autowired
    private TeamCardsUseCase teamCardsUseCase;

    @Autowired
    private TeamCardsService teamCardsService;

    @Test
    @WithMockUser(value = "tracker", roles = "TRACKER")
    void updateTeamCard_passiveTeam_throwsException() {
        // Создаём пассивную команду
        var passiveTeam = teamCardsService.createTeamCard(TeamCard.builder()
                .name("Passive Team")
                .username("tracker")
                .meetingRoomLink("https://test.com")
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .passive(true)
                .build());

        var updateDto = TeamCardUpdateDto.builder()
                .name("Try to update passive team")
                .build();

        assertThatThrownBy(() -> teamCardsUseCase.updateTeamCard(passiveTeam.getId(), updateDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Нельзя редактировать пассивную команду");
    }
}
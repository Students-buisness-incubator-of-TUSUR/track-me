package net.trackme.backend.usecases;

import net.trackme.backend.domain.ReadinessLevel;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardUpdateDto;
import net.trackme.backend.services.nti.NtiMarketService;
import net.trackme.backend.services.teamcard.TeamCardsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamCardsUseCaseTest {

    @Mock
    private TeamCardsService teamCardsService;

    @Mock
    private NtiMarketService ntiMarketService;

    @InjectMocks
    private TeamCardsUseCase teamCardsUseCase;

    @Test
    void updateTeamCard_shouldThrowExceptionWhenPassive() {
        // Arrange
        var teamCardId = UUID.randomUUID();
        var existingTeamCard = TeamCard.builder()
                .id(teamCardId)
                .name("Passive Team")
                .passive(true)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .build();

        var updateDto = TeamCardUpdateDto.builder()
                .name("Updated Name")
                .readinessLevel("3-5")
                .ntiMarketIds(new ArrayList<>())
                .build();

        when(teamCardsService.getTeamCard(teamCardId)).thenReturn(existingTeamCard);
        when(ntiMarketService.getNtiMarkets(any())).thenReturn(new ArrayList<>());

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                teamCardsUseCase.updateTeamCard(teamCardId, updateDto)
        );
    }

    @Test
    void updateTeamCard_shouldAllowEditWhenNotPassive() throws Exception {
        // Arrange
        var teamCardId = UUID.randomUUID();
        var existingTeamCard = TeamCard.builder()
                .id(teamCardId)
                .name("Active Team")
                .passive(false)
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .build();

        var updateDto = TeamCardUpdateDto.builder()
                .name("Updated Name")
                .readinessLevel("3-5")
                .ntiMarketIds(new ArrayList<>())
                .build();

        var updatedTeamCard = TeamCard.builder()
                .id(teamCardId)
                .name("Updated Name")
                .passive(false)
                .readinessLevel(ReadinessLevel.LEVEL_2)
                .build();

        when(teamCardsService.getTeamCard(teamCardId)).thenReturn(existingTeamCard);
        when(ntiMarketService.getNtiMarkets(any())).thenReturn(new ArrayList<>());
        when(teamCardsService.updateTeamCard(eq(teamCardId), any(TeamCard.class))).thenReturn(updatedTeamCard);

        // Act
        var result = teamCardsUseCase.updateTeamCard(teamCardId, updateDto);

        // Assert
        assertEquals("Updated Name", result.name());
        verify(teamCardsService).updateTeamCard(eq(teamCardId), any(TeamCard.class));
    }
}
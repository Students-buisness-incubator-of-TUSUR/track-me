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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamCardsUseCaseTest {

    @Mock
    private TeamCardsService teamCardsService;

    @Mock
    private NtiMarketService ntiMarketService;

    @InjectMocks
    private TeamCardsUseCase teamCardsUseCase;

    @Test
    void updateTeamCard_shouldThrowExceptionWhenPassiveAndNotAdmin() {
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

        // Устанавливаем роль TRACKER (не админ)
        var auth = new UsernamePasswordAuthenticationToken(
                "user", null, List.of(new SimpleGrantedAuthority("ROLE_TRACKER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                teamCardsUseCase.updateTeamCard(teamCardId, updateDto)
        );
    }

    @Test
    void updateTeamCard_shouldAllowEditWhenPassiveAndAdmin() throws Exception {
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

        var updatedTeamCard = TeamCard.builder()
                .id(teamCardId)
                .name("Updated Name")
                .passive(true)
                .readinessLevel(ReadinessLevel.LEVEL_2)
                .build();

        when(teamCardsService.getTeamCard(teamCardId)).thenReturn(existingTeamCard);
        when(ntiMarketService.getNtiMarkets(any())).thenReturn(new ArrayList<>());
        when(teamCardsService.updateTeamCard(eq(teamCardId), any(TeamCard.class))).thenReturn(updatedTeamCard);

        // Устанавливаем роль ADMIN
        var auth = new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        var result = teamCardsUseCase.updateTeamCard(teamCardId, updateDto);

        // Assert
        assertEquals("Updated Name", result.name());
        verify(teamCardsService).updateTeamCard(eq(teamCardId), any(TeamCard.class));
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

        // Устанавливаем роль TRACKER
        var auth = new UsernamePasswordAuthenticationToken(
                "user", null, List.of(new SimpleGrantedAuthority("ROLE_TRACKER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        var result = teamCardsUseCase.updateTeamCard(teamCardId, updateDto);

        // Assert
        assertEquals("Updated Name", result.name());
        verify(teamCardsService).updateTeamCard(eq(teamCardId), any(TeamCard.class));
    }
}
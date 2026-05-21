/*package net.trackme.backend.usecases;

import net.trackme.backend.domain.ReadinessLevel;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardUpdateDto;
import net.trackme.backend.services.teamcard.TeamCardsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
                .passive(true)  // 👈 Пассивная команда
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .build();

        var updateDto = TeamCardUpdateDto.builder()
                .name("Updated Name")
                .readinessLevel("3-5")
                .build();

        // Мокаем сервис
        when(teamCardsService.getTeamCard(teamCardId)).thenReturn(existingTeamCard);

        // Мокаем SecurityContext для роли TRACKER (не админ)
        var authentication = mock(Authentication.class);
        when(authentication.getAuthorities()).thenReturn(List.of(
                new SimpleGrantedAuthority("ROLE_TRACKER")
        ));
        SecurityContextHolder.setContext(mock(SecurityContext.class));
        when(SecurityContextHolder.getContext().getAuthentication()).thenReturn(authentication);

        // Act & Assert
        assertThatThrownBy(() -> teamCardsUseCase.updateTeamCard(teamCardId, updateDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Нельзя редактировать пассивную команду");
    }

    @Test
    void updateTeamCard_shouldAllowEditWhenPassiveAndAdmin() {
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
                .ntiMarketIds(List.of())
                .build();

        var updatedTeamCard = TeamCard.builder()
                .id(teamCardId)
                .name("Updated Name")
                .passive(true)
                .readinessLevel(ReadinessLevel.LEVEL_2)
                .build();

        when(teamCardsService.getTeamCard(teamCardId)).thenReturn(existingTeamCard);
        when(ntiMarketService.getNtiMarkets(any())).thenReturn(List.of());
        when(teamCardsService.updateTeamCard(eq(teamCardId), any(TeamCard.class))).thenReturn(updatedTeamCard);

        // Мокаем SecurityContext для роли ADMIN
        var authentication = mock(Authentication.class);
        when(authentication.getAuthorities()).thenReturn(List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN")
        ));
        SecurityContextHolder.setContext(mock(SecurityContext.class));
        when(SecurityContextHolder.getContext().getAuthentication()).thenReturn(authentication);

        // Act
        var result = teamCardsUseCase.updateTeamCard(teamCardId, updateDto);

        // Assert
        assertThat(result.name()).isEqualTo("Updated Name");
        verify(teamCardsService).updateTeamCard(eq(teamCardId), any(TeamCard.class));
    }

    @Test
    void updateTeamCard_shouldAllowEditWhenNotPassiveAndTracker() {
        // Arrange
        var teamCardId = UUID.randomUUID();
        var existingTeamCard = TeamCard.builder()
                .id(teamCardId)
                .name("Active Team")
                .passive(false)  // 👈 Не пассивная
                .readinessLevel(ReadinessLevel.LEVEL_1)
                .build();

        var updateDto = TeamCardUpdateDto.builder()
                .name("Updated Name")
                .readinessLevel("3-5")
                .ntiMarketIds(List.of())
                .build();

        var updatedTeamCard = TeamCard.builder()
                .id(teamCardId)
                .name("Updated Name")
                .passive(false)
                .readinessLevel(ReadinessLevel.LEVEL_2)
                .build();

        when(teamCardsService.getTeamCard(teamCardId)).thenReturn(existingTeamCard);
        when(ntiMarketService.getNtiMarkets(any())).thenReturn(List.of());
        when(teamCardsService.updateTeamCard(eq(teamCardId), any(TeamCard.class))).thenReturn(updatedTeamCard);

        // Мокаем SecurityContext для роли TRACKER (не админ)
        var authentication = mock(Authentication.class);
        when(authentication.getAuthorities()).thenReturn(List.of(
                new SimpleGrantedAuthority("ROLE_TRACKER")
        ));
        SecurityContextHolder.setContext(mock(SecurityContext.class));
        when(SecurityContextHolder.getContext().getAuthentication()).thenReturn(authentication);

        // Act
        var result = teamCardsUseCase.updateTeamCard(teamCardId, updateDto);

        // Assert
        assertThat(result.name()).isEqualTo("Updated Name");
        verify(teamCardsService).updateTeamCard(eq(teamCardId), any(TeamCard.class));
    }
}*/
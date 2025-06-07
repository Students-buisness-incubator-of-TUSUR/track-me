package net.trackme.backend.rest.api.teamcard;

import lombok.RequiredArgsConstructor;
import net.trackme.backend.commons.filters.FilterRequest;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardCreateOrUpdateDto;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardDto;
import net.trackme.backend.usecases.TeamCardsUseCase;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TeamCardsRestControllerImpl implements TeamCardsRestController {

  private final TeamCardsUseCase teamCardsUseCase;

  @Override
  public ResponseEntity<TeamCardDto> createTeamCard(UUID streamId, TeamCardCreateOrUpdateDto dto,
                                                    Authentication authentication) {
    var createTeamCardDto = teamCardsUseCase.createTeamCard(dto, streamId, authentication);
    return ResponseEntity.ok(createTeamCardDto);
  }

  @Override
  public ResponseEntity<TeamCardDto> updateTeamCard(UUID teamCardId,
                                                    TeamCardCreateOrUpdateDto dto) {
    var updatedTeamCard = teamCardsUseCase.updateTeamCard(teamCardId, dto);
    return ResponseEntity.ok(updatedTeamCard);
  }

  @Override
  public ResponseEntity<PagedModel<TeamCardDto>> getTeamCards(FilterRequest filterRequest,
                                                              Pageable pageable,
                                                              Authentication authentication) {
    var page = teamCardsUseCase.getTeamCards(filterRequest.filters(), authentication, pageable);
    return ResponseEntity.ok(new PagedModel<>(page));
  }

  @Override
  public ResponseEntity<TeamCardDto> getTeamCard(UUID id) {
    var teamCardDto = teamCardsUseCase.getTeamCard(id);
    return ResponseEntity.ok(teamCardDto);
  }

  @Override
  public ResponseEntity<Integer> getTeamCardCount(UUID streamId) {
    var count = teamCardsUseCase.getTeamCardCount(streamId);
    return ResponseEntity.ok(count);
  }

  @Override
  public ResponseEntity<Void> deleteTeamCard(UUID id) {
    teamCardsUseCase.deleteTeamCard(id);
    return ResponseEntity.noContent().build();
  }
}

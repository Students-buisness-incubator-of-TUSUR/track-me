package net.akarmanov.projectplace.rest.api.teamcard;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.filters.FilterRequest;
import net.akarmanov.projectplace.rest.api.teamcard.dto.TeamCardCreateOrUpdateDto;
import net.akarmanov.projectplace.rest.api.teamcard.dto.TeamCardDto;
import net.akarmanov.projectplace.usecases.UserTeamCardsUseCase;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TeamCardsRestControllerImpl implements TeamCardsRestController {

  private final UserTeamCardsUseCase userTeamCardsUseCase;

  @Override
  public ResponseEntity<TeamCardDto> createTeamCard(UUID streamId, TeamCardCreateOrUpdateDto dto) {
    var createTeamCardDto = userTeamCardsUseCase.createTeamCard(dto, streamId);
    return ResponseEntity.ok(createTeamCardDto);
  }

  @Override
  public ResponseEntity<TeamCardDto> updateTeamCard(UUID teamCardId, TeamCardCreateOrUpdateDto dto) {
    var updatedTeamCard = userTeamCardsUseCase.updateTeamCard(teamCardId, dto);
    return ResponseEntity.ok(updatedTeamCard);
  }

  @Override
  public ResponseEntity<PagedModel<TeamCardDto>> getTeamCards(FilterRequest filterRequest, Pageable pageable) {
    var page = userTeamCardsUseCase.getTeamCards(filterRequest.filters(), pageable);
    return ResponseEntity.ok(new PagedModel<>(page));
  }

  @Override
  public ResponseEntity<TeamCardDto> getTeamCard(UUID id) {
    var teamCardDto = userTeamCardsUseCase.getTeamCard(id);
    return ResponseEntity.ok(teamCardDto);
  }

  @Override
  public ResponseEntity<Integer> getTeamCardCount(UUID streamId) {
    var count = userTeamCardsUseCase.getTeamCardCount(streamId);
    return ResponseEntity.ok(count);
  }

  @Override
  public ResponseEntity<Void> deleteTeamCard(UUID id) {
    userTeamCardsUseCase.deleteTeamCard(id);
    return ResponseEntity.noContent().build();
  }
}

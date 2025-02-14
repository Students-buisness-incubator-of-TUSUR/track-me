package net.akarmanov.projectplace.usecases;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.domain.spec.TeamCardSpecification;
import net.akarmanov.projectplace.mapping.TeamCardMapper;
import net.akarmanov.projectplace.models.Filter;
import net.akarmanov.projectplace.rest.api.teamcard.dto.TeamCardCreateOrUpdateDto;
import net.akarmanov.projectplace.rest.api.teamcard.dto.TeamCardDto;
import net.akarmanov.projectplace.services.stream.StreamService;
import net.akarmanov.projectplace.services.teamcard.TeamCardsService;
import net.akarmanov.projectplace.services.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserTeamCardsUseCaseImpl implements UserTeamCardsUseCase {

  private final TeamCardsService teamCardsService;

  private final StreamService streamService;

  private final UserService userService;

  private final TeamCardMapper teamCardMapper;

  @Override
  @Transactional
  public TeamCardDto createTeamCard(TeamCardCreateOrUpdateDto teamCard) {
    var stream = streamService.getCurrentStream();
    var teamCardEntity = teamCardMapper.mapToEntity(teamCard);
    teamCardEntity.setUser(userService.getCurrentUser());
    var createdTeamCard = teamCardsService.createTeamCard(teamCardEntity);
    stream.addTeamCard(createdTeamCard);
    streamService.save(stream);
    return teamCardMapper.mapToDto(createdTeamCard);
  }

  @Override
  public TeamCardDto updateTeamCard(UUID teamCardId, TeamCardCreateOrUpdateDto createOrUpdateDto) {
    var teamCard = teamCardMapper.mapToEntity(createOrUpdateDto);
    var updatedTeamCard = teamCardsService.updateTeamCard(teamCardId, teamCard);
    return teamCardMapper.mapToDto(updatedTeamCard);
  }

  @Override
  public Page<TeamCardDto> getTeamCards(List<Filter> filters, Pageable pageable) {
    var user = userService.getCurrentUser();
    var specs = TeamCardSpecification.withFilters(filters);
    var page = teamCardsService.getTeamCards(specs, pageable, user.getId());
    return page.map(teamCardMapper::mapToDto);
  }

  @Override
  @PreAuthorize("hasPermission(#teamCardId, 'net.akarmanov.projectplace.domain.TeamCard', 'READ')")
  public TeamCardDto getTeamCard(UUID teamCardId) {
    var teamCard = teamCardsService.getTeamCard(teamCardId);
    return teamCardMapper.mapToDto(teamCard);
  }

  @Override
  public void deleteTeamCard(UUID id) {
    teamCardsService.deleteTeamCard(id);
  }
}

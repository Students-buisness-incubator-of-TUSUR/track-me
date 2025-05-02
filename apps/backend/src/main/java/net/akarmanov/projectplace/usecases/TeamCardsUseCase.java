package net.akarmanov.projectplace.usecases;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.commons.filters.Filter;
import net.akarmanov.projectplace.mapping.TeamCardMapper;
import net.akarmanov.projectplace.models.TeamCardStatus;
import net.akarmanov.projectplace.rest.api.teamcard.dto.TeamCardCreateOrUpdateDto;
import net.akarmanov.projectplace.rest.api.teamcard.dto.TeamCardDto;
import net.akarmanov.projectplace.services.nti.NtiMarketService;
import net.akarmanov.projectplace.services.stream.MutableStreamService;
import net.akarmanov.projectplace.services.teamcard.TeamCardsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static net.akarmanov.projectplace.domain.spec.TeamCardSpecification.userEquals;
import static net.akarmanov.projectplace.domain.spec.TeamCardSpecification.withFilters;

@Component
@RequiredArgsConstructor
public class TeamCardsUseCase {

  private final TeamCardsService teamCardsService;

  private final MutableStreamService streamService;

  private final TeamCardMapper teamCardMapper;

  private final NtiMarketService ntiMarketService;

  @Transactional
  public TeamCardDto createTeamCard(TeamCardCreateOrUpdateDto teamCardDto, UUID streamId,
                                    Authentication authentication) {
    var username = authentication.getName();
    var ntiMarketId = teamCardDto.ntiMarketId();

    var stream = streamService.findActive(streamId);
    var teamCardEntity = teamCardMapper.mapToEntity(teamCardDto);
    var ntiMarket = ntiMarketService.getNtiMarket(ntiMarketId);

    teamCardEntity.setNtiMarket(ntiMarket);
    teamCardEntity.setUsername(username);
    teamCardEntity.addStream(stream);
    teamCardEntity.setStatus(TeamCardStatus.OK);

    var createdTeamCard = teamCardsService.createTeamCard(teamCardEntity);
    return teamCardMapper.mapToDto(createdTeamCard);
  }


  public TeamCardDto updateTeamCard(UUID teamCardId, TeamCardCreateOrUpdateDto createOrUpdateDto) {
    var ntMarketId = createOrUpdateDto.ntiMarketId();
    var teamCard = teamCardMapper.mapToEntity(createOrUpdateDto);
    teamCard.setNtiMarket(ntiMarketService.getNtiMarket(ntMarketId));
    var updatedTeamCard = teamCardsService.updateTeamCard(teamCardId, teamCard);
    return teamCardMapper.mapToDto(updatedTeamCard);
  }

  public Page<TeamCardDto> getTeamCards(List<Filter> filters,
                                        Authentication authentication,
                                        Pageable pageable) {
    var page = teamCardsService.getTeamCards(withFilters(filters)
            .and(userEquals(authentication.getName())),
        pageable);
    return page.map(teamCardMapper::mapToDto);
  }

  @PreAuthorize("hasPermission(#teamCardId, 'net.akarmanov.projectplace.domain.TeamCard', 'READ')")
  public TeamCardDto getTeamCard(UUID teamCardId) {
    var teamCard = teamCardsService.getTeamCard(teamCardId);
    return teamCardMapper.mapToDto(teamCard);
  }

  public void deleteTeamCard(UUID id) {
    teamCardsService.deleteTeamCard(id);
  }

  public Integer getTeamCardCount(UUID streamId) {
    return teamCardsService.getTeamCardCount(streamId);
  }
}

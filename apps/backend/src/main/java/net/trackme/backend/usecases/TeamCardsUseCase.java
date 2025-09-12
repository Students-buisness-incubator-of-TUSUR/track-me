package net.trackme.backend.usecases;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.trackme.backend.mapping.TeamCardMapper;
import net.trackme.backend.models.TeamCardStatus;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardCreateOrUpdateDto;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardDto;
import net.trackme.backend.services.nti.NtiMarketService;
import net.trackme.backend.services.stream.MutableStreamService;
import net.trackme.backend.services.teamcard.TeamCardsService;
import net.trackme.commons.filters.Filter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static net.trackme.backend.domain.spec.TeamCardSpecification.userEquals;
import static net.trackme.backend.domain.spec.TeamCardSpecification.withFilters;

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
        var ntiMarketIds = teamCardDto.ntiMarketIds();

        var stream = streamService.findActive(streamId);
        var teamCardEntity = teamCardMapper.mapToEntity(teamCardDto);
        var ntiMarkets = ntiMarketService.getNtiMarkets(ntiMarketIds);

        teamCardEntity.setUsername(username);
        teamCardEntity.setNtiMarkets(ntiMarkets);
        teamCardEntity.addStream(stream);
        teamCardEntity.setStatus(TeamCardStatus.OK);

        var createdTeamCard = teamCardsService.createTeamCard(teamCardEntity);
        return teamCardMapper.mapToDto(createdTeamCard);
    }


    public TeamCardDto updateTeamCard(UUID teamCardId,
                                      TeamCardCreateOrUpdateDto createOrUpdateDto) {
        var ntiMarketIds = createOrUpdateDto.ntiMarketIds();
        var teamCard = teamCardMapper.mapToEntity(createOrUpdateDto);
        var ntiMarkets = ntiMarketService.getNtiMarkets(ntiMarketIds);

        teamCard.setNtiMarkets(ntiMarkets);
        var updatedTeamCard = teamCardsService.updateTeamCard(teamCardId, teamCard);
        return teamCardMapper.mapToDto(updatedTeamCard);
    }

    public Page<TeamCardDto> getTeamCards(List<Filter> filters,
                                          Authentication authentication,
                                          Pageable pageable) {
        var page = teamCardsService.getTeamCards(
                withFilters(filters)
                        .and(userEquals(authentication.getName())),
                pageable);
        return page.map(teamCardMapper::mapToDto);
    }

    @PreAuthorize("hasPermission(#teamCardId, 'net.trackme.backend.domain.TeamCard', 'READ')")
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

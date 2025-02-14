package net.akarmanov.projectplace.services.teamcard;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.domain.TeamCard;
import net.akarmanov.projectplace.models.TeamCardStatus;
import net.akarmanov.projectplace.repos.TeamCardsRepository;
import net.akarmanov.projectplace.services.acl.AclService;
import net.akarmanov.projectplace.services.exceptions.TeamCardNotFoundException;
import net.akarmanov.projectplace.services.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static net.akarmanov.projectplace.domain.spec.TeamCardSpecification.userEquals;

@Service
@Transactional
@RequiredArgsConstructor
public class DomainTeamCardsService implements TeamCardsService {

  private final TeamCardsRepository teamCardsRepository;

  private final UserService userService;

  private final AclService aclService;

  @Override
  public TeamCard createTeamCard(TeamCard createTeamCard) {
    var user = userService.getCurrentUser();
    createTeamCard.setStatus(TeamCardStatus.OK);
    createTeamCard.setUser(user);
    createTeamCard = teamCardsRepository.save(createTeamCard);
    aclService.createAcl(createTeamCard);
    return createTeamCard;
  }

  @Override
  @PreAuthorize("hasPermission(#teamCardId, 'net.akarmanov.projectplace.domain.TeamCard', 'WRITE')")
  public TeamCard updateTeamCard(UUID teamCardId, TeamCard teamCardDto) {
    var teamCard = get(teamCardId);
    updateTeamCard(teamCardDto, teamCard);
    teamCard = teamCardsRepository.save(teamCard);
    return teamCardsRepository.save(teamCard);
  }

  private TeamCard get(UUID teamCardId) {
    return teamCardsRepository.findById(teamCardId)
        .orElseThrow(() -> new TeamCardNotFoundException(teamCardId));
  }

  @Override
  public Page<TeamCard> getTeamCards(Specification<TeamCard> specification,
                                     Pageable pageable,
                                     UUID userId) {
    return teamCardsRepository.findAll(specification
        .and(userEquals(userId)), pageable);
  }

  @Override
  public TeamCard getTeamCard(UUID id) {
    return teamCardsRepository.findById(id)
        .orElseThrow(() -> new TeamCardNotFoundException(id));
  }

  @Override
  @PreAuthorize("hasPermission(#id, 'net.akarmanov.projectplace.domain.TeamCard', 'DELETE')")
  public void deleteTeamCard(UUID id) {
    teamCardsRepository.deleteById(id);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public TeamCard createTeamCard(TeamCard create, UUID userId) {
    var user = userService.getUser(userId);
    create.setUser(user);
    create = teamCardsRepository.save(create);
    aclService.createAcl(create, user.getUsername());
    return create;
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public TeamCard updateTeamCard(UUID teamCardId, TeamCard teamCardDto, UUID userId) {
    var teamCard = get(teamCardId, userId);
    updateTeamCard(teamCardDto, teamCard);
    var user = userService.getUser(userId);
    teamCard.setUser(user);
    teamCard = teamCardsRepository.save(teamCard);
    aclService.updateAcl(teamCard, user.getUsername());
    return teamCard;
  }

  @Override
  public Page<TeamCard> findAll(Specification<TeamCard> specification, Pageable pageable) {
    return teamCardsRepository.findAll(specification, pageable);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public TeamCard getTeamCard(UUID id, UUID userId) {
    return get(id, userId);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public void deleteTeamCard(UUID id, UUID userId) {
    var teamCard = get(id, userId);
    aclService.deleteAcl(teamCard);
    teamCardsRepository.deleteByIdAndUserId(id, userId);
  }

  private TeamCard get(UUID teamCardId, UUID userId) {
    return teamCardsRepository.findByIdAndUserId(teamCardId, userId)
        .orElseThrow(() -> new TeamCardNotFoundException(teamCardId, userId));
  }

  private void updateTeamCard(TeamCard source, TeamCard target) {
    if (source.getName() != null) {
      target.setName(source.getName());
    }
    if (source.getDescription() != null) {
      target.setDescription(source.getDescription());
    }
    if (source.getStatus() != null) {
      target.setStatus(source.getStatus());
    }
  }
}

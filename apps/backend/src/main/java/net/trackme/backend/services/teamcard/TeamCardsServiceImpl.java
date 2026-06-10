package net.trackme.backend.services.teamcard;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.messaging.internal.TeamCardChangedInternalEvent;
import net.trackme.backend.messaging.internal.TeamCardStreamAddedInternalEvent;
import net.trackme.backend.models.TeamCardStatus;
import net.trackme.backend.repos.TeamCardsRepository;
import net.trackme.backend.services.exceptions.TeamCardNotFoundException;
import net.trackme.backend.services.stream.StreamService;
import net.trackme.commons.acl.AclService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;
import java.util.List;  

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TeamCardsServiceImpl implements TeamCardsService {

    /**
     * Репозиторий карточек команд.
     */
    private final TeamCardsRepository teamCardsRepository;

    /**
     * Сервис потоков.
     */
    private final StreamService streamService;

    /**
     * Сервис ACL.
     */
    private final AclService aclService;

    /**
     * Публикатор внутренних системных ивентов.
     */
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public TeamCard createTeamCard(TeamCard createTeamCard) {
        createTeamCard.setStatus(TeamCardStatus.OK);
        if (createTeamCard.getTrackerFullName() == null) {
            createTeamCard.setTrackerFullName(createTeamCard.getUsername());
        }
        createTeamCard = teamCardsRepository.save(createTeamCard);
        var username = SecurityContextHolder.getContext().getAuthentication().getName();
        aclService.createAclForUser(createTeamCard, username);
        return createTeamCard;
    }

    @Override
    @PreAuthorize("hasPermission(#teamCardId, 'net.trackme.backend.domain.TeamCard', 'WRITE')")
    public TeamCard updateTeamCard(UUID teamCardId, TeamCard teamCardDto) {
        var teamCard = get(teamCardId);
        updateTeamCard(teamCardDto, teamCard);
        teamCard = teamCardsRepository.save(teamCard);

        eventPublisher.publishEvent(new TeamCardChangedInternalEvent(
            teamCardId,
            teamCard.getName(),
            teamCard.getUsername(),
            teamCard.getTrackerFullName()
        ));

        return teamCard;
    }

    @Override
    public Page<TeamCard> getTeamCards(Specification<TeamCard> specification,
                                       Pageable pageable) {
        return teamCardsRepository.findAll(specification, pageable);
    }

    @Override
    public TeamCard getTeamCard(UUID id) {
        return get(id);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'net.trackme.backend.domain.TeamCard', 'DELETE')")
    public void deleteTeamCard(UUID id) {
        teamCardsRepository.deleteById(id);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public TeamCard createTeamCard(TeamCard create, String username) {
        create.setUsername(username);
        create.setStatus(TeamCardStatus.OK);
        if (create.getTrackerFullName() == null) {
            create.setTrackerFullName(username);
        }
        create = teamCardsRepository.save(create);
        aclService.createAclForUser(
                create, username, SecurityContextHolder.getContext().getAuthentication().getName());
        return create;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public TeamCard updateTeamCard(UUID teamCardId,
                                   TeamCard teamCardDto,
                                   UUID streamId,
                                   String username) {
        var teamCard = get(teamCardId);
        updateTeamCard(teamCardDto, teamCard);
        if (streamId != null) {
            var stream = streamService.getById(streamId);
            teamCard.addStream(stream);
            eventPublisher.publishEvent(new TeamCardStreamAddedInternalEvent(
                teamCardId,
                streamId
            ));
        }
        if (username != null) {
            teamCard.setUsername(username);
            aclService.updateAclOwner(teamCard, username);
        }

        teamCard = teamCardsRepository.save(teamCard);
        eventPublisher.publishEvent(new TeamCardChangedInternalEvent(
            teamCardId,
            teamCard.getName(),
            teamCard.getUsername(),
            teamCard.getTrackerFullName()
        ));

        return teamCard;
    }

    @Override
    public Page<TeamCard> findAll(Specification<TeamCard> specification, Pageable pageable) {
        return teamCardsRepository.findAll(specification, pageable);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public TeamCard getTeamCard(UUID id, String username) {
        return get(id, username);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteTeamCard(UUID id, String username) {
        var teamCard = get(id, username);
        aclService.deleteAcl(teamCard);
        teamCardsRepository.deleteByIdAndUsername(id, username);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public TeamCard createTeamCard(TeamCard teamCard, UUID streamId, String username) {
        if (streamId != null) {
            var stream = streamService.getById(streamId);
            teamCard.addStream(stream);
        }
        return createTeamCard(teamCard, username);
    }

    @Override
    public Integer getTeamCardCount(UUID streamId) {
        return teamCardsRepository.countByStreamsIdIn(Collections.singletonList(streamId));
    }

    private TeamCard get(UUID teamCardId) {
        return teamCardsRepository.findById(teamCardId)
                .orElseThrow(() -> new TeamCardNotFoundException(teamCardId));
    }

    private TeamCard get(UUID teamCardId, String username) {
        return teamCardsRepository.findByIdAndUsername(teamCardId, username)
                .orElseThrow(() -> new TeamCardNotFoundException(teamCardId, username));
    }

    private void updateTeamCard(TeamCard source, TeamCard target) {
        if (source.getName() != null) {
            target.setName(source.getName());
        }
        if (source.getDescription() != null) {
            target.setDescription(source.getDescription());
        }
        if (source.getTrackerFullName() != null) {
            target.setTrackerFullName(source.getTrackerFullName());
        }
        if (source.getMeetingRoomLink() != null) {
            target.setMeetingRoomLink(source.getMeetingRoomLink());
        }
        if (source.getStatus() != null) {
            target.setStatus(source.getStatus());
        }
        if (!source.getNtiMarkets().isEmpty()) {
            target.setNtiMarkets(source.getNtiMarkets());
        }
        if (source.getReadinessLevel() != null) {
            target.setReadinessLevel(source.getReadinessLevel());
        }
        if (source.getPassive() != null) {
            target.setPassive(source.getPassive());
        }
    }

    @Override
    @Transactional
    public List<String> getTeamCardNamesByUser(String username) {
        List<TeamCard> teams = teamCardsRepository.findByUsername(username);
        return teams.stream()
            .map(TeamCard::getName)
            .toList();
    }

    @Override
    public List<TeamCard> getTeamCardsByUser(String username) {
        return teamCardsRepository.findByUsername(username);
    }

    /**
     * Переназначает команды от одного трекера другому.
     * Обновляет как username, так и trackerFullName для консистентности данных.
     *
     * @param fromUsername старый username трекера
     * @param toUsername новый username трекера
     * @param toUserFullName полное имя нового трекера (если известно)
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void reassignTeams(String fromUsername, String toUsername, String toUserFullName) {
        List<TeamCard> teams = teamCardsRepository.findByUsername(fromUsername);
        log.info("Reassigning {} teams from '{}' to '{}'", teams.size(), fromUsername, toUsername);
    
        // Используем полное имя нового трекера, если не предоставлено - используем username
        String newTrackerFullName = (toUserFullName != null && !toUserFullName.isBlank()) 
            ? toUserFullName 
            : toUsername;
        
        for (TeamCard team : teams) {
            String oldUsername = team.getUsername();
            String oldFullName = team.getTrackerFullName();
            
            // Обновляем оба поля для консистентности
            team.setUsername(toUsername);
            team.setTrackerFullName(newTrackerFullName);
            
            // Обновляем ACL владельца
            aclService.updateAclOwner(team, toUsername);
            
            // Публикуем событие с обновленными данными
            eventPublisher.publishEvent(new TeamCardChangedInternalEvent(
                team.getId(),
                team.getName(),
                toUsername,
                newTrackerFullName
            ));
            
            log.debug("Reassigned team '{}': {} -> {}, fullName: '{}' -> '{}'", 
                team.getId(), oldUsername, toUsername, oldFullName, newTrackerFullName);
        }

        teamCardsRepository.saveAll(teams);
        log.info("Successfully reassigned {} teams", teams.size());
    }
}
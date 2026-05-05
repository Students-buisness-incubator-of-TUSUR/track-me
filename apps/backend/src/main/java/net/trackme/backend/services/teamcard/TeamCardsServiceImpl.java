    package net.trackme.backend.services.teamcard;

<<<<<<< HEAD
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.backend.domain.NTIMarket;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.models.TeamCardStatus;
import net.trackme.backend.repos.TeamCardsRepository;
import net.trackme.backend.services.exceptions.TeamCardNotFoundException;
import net.trackme.backend.services.exceptions.ValidationException;
import net.trackme.backend.services.stream.StreamService;
import net.trackme.commons.acl.AclService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TeamCardsServiceImpl implements TeamCardsService {

    private final TeamCardsRepository teamCardsRepository;
    private final StreamService streamService;
    private final AclService aclService;

    /**
     * Проверяет, что хотя бы один рынок НТИ команды соответствует рынкам НТИ потока.
     */
    private void validateTeamMarketsAgainstStream(List<NTIMarket> teamMarkets, Stream stream) {
        if (teamMarkets == null || teamMarkets.isEmpty()) {
            throw new ValidationException("Необходимо выбрать хотя бы один рынок НТИ");
        }
        
        List<UUID> streamMarketIds = stream.getNtiMarkets().stream()
                .map(NTIMarket::getId)
                .toList();
        
        boolean hasMatch = teamMarkets.stream()
                .anyMatch(market -> streamMarketIds.contains(market.getId()));
        
        if (!hasMatch) {
            throw new ValidationException(
                "Хотя бы один рынок НТИ команды должен соответствовать рынкам НТИ акселерационного потока."
            );
        }
    }

    @Override
    public TeamCard createTeamCard(TeamCard createTeamCard) {
        // Валидация соответствия рынков потоку
        if (createTeamCard.getStreams() != null && !createTeamCard.getStreams().isEmpty()) {
            Stream stream = createTeamCard.getStreams().iterator().next();
            validateTeamMarketsAgainstStream(createTeamCard.getNtiMarkets(), stream);
        }
        
        createTeamCard.setStatus(TeamCardStatus.OK);
        createTeamCard = teamCardsRepository.save(createTeamCard);
        var username = SecurityContextHolder.getContext().getAuthentication().getName();
        aclService.createAclForUser(createTeamCard, username);
        return createTeamCard;
    }

    @Override
    @PreAuthorize("hasPermission(#teamCardId, 'net.trackme.backend.domain.TeamCard', 'WRITE')")
    public TeamCard updateTeamCard(UUID teamCardId, TeamCard teamCardDto) {
        var teamCard = get(teamCardId);
        
        // Валидация при обновлении рынков
        if (teamCardDto.getNtiMarkets() != null && !teamCardDto.getNtiMarkets().isEmpty()) {
            if (teamCard.getStreams() != null && !teamCard.getStreams().isEmpty()) {
                Stream stream = teamCard.getStreams().iterator().next();
                validateTeamMarketsAgainstStream(teamCardDto.getNtiMarkets(), stream);
            }
        }
        
        updateTeamCard(teamCardDto, teamCard);
        return teamCardsRepository.save(teamCard);
    }

    @Override
    public Page<TeamCard> getTeamCards(Specification<TeamCard> specification, Pageable pageable) {
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
        create = teamCardsRepository.save(create);
        aclService.createAclForUser(create, username, SecurityContextHolder.getContext().getAuthentication().getName());
        return create;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public TeamCard updateTeamCard(UUID teamCardId, TeamCard teamCardDto, UUID streamId, String username) {
        var teamCard = get(teamCardId);
        
        if (streamId != null) {
            var stream = streamService.getById(streamId);
            // Валидация для нового потока
            if (teamCardDto.getNtiMarkets() != null && !teamCardDto.getNtiMarkets().isEmpty()) {
                validateTeamMarketsAgainstStream(teamCardDto.getNtiMarkets(), stream);
            }
            teamCard.addStream(stream);
        } else {
            // Если поток не меняется, проверяем по текущему потоку
            if (teamCardDto.getNtiMarkets() != null && !teamCardDto.getNtiMarkets().isEmpty()) {
                if (teamCard.getStreams() != null && !teamCard.getStreams().isEmpty()) {
                    Stream stream = teamCard.getStreams().iterator().next();
                    validateTeamMarketsAgainstStream(teamCardDto.getNtiMarkets(), stream);
                }
            }
        }
        
        updateTeamCard(teamCardDto, teamCard);
        
        if (username != null) {
            teamCard.setUsername(username);
            aclService.updateAclOwner(teamCard, username);
=======
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
                teamCard.getUsername()
            ));

            return teamCard;
>>>>>>> upstream/develop
        }

<<<<<<< HEAD
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
            // Валидация при создании админом
            validateTeamMarketsAgainstStream(teamCard.getNtiMarkets(), stream);
            teamCard.addStream(stream);
=======
        @Override
        public Page<TeamCard> getTeamCards(Specification<TeamCard> specification,
                                           Pageable pageable) {
            return teamCardsRepository.findAll(specification, pageable);
>>>>>>> upstream/develop
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
                teamCard.getUsername()
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
<<<<<<< HEAD
        if (source.getNtiMarkets() != null && !source.getNtiMarkets().isEmpty()) {
            target.setNtiMarkets(source.getNtiMarkets());
=======

        @Override
        @Transactional
        @PreAuthorize("hasRole('ADMIN')")
        public TeamCard createTeamCard(TeamCard teamCard, UUID streamId, String username) {
            if (streamId != null) {
                var stream = streamService.getById(streamId);
                teamCard.addStream(stream);
            }
            return createTeamCard(teamCard, username);
>>>>>>> upstream/develop
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
        }
    }
<<<<<<< HEAD
}
=======
>>>>>>> upstream/develop

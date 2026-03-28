package net.trackme.meetingservice.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.meetingservice.dao.MeetingMetadataRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.services.integration.backend.BackendApiClient;
import net.trackme.meetingservice.services.integration.backend.dto.StreamDto;
import net.trackme.meetingservice.services.integration.backend.dto.TeamCardDto;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Сервис для разового заполнения денормализованных данных в старых встречах.
 * Подтягивает названия команд, трекеров и списки потоков из основного бэкенда.
 *
 */
@Component
@Slf4j
@Profile("!test")
public class MeetingDataBackfiller  {
    private final AtomicBoolean isStarted = new AtomicBoolean(false);
    private final BackendApiClient backendApiClient;
    private final MeetingMetadataRepository metadataRepository;

    public MeetingDataBackfiller(MeetingMetadataRepository metadataRepository, @Lazy BackendApiClient backendApiClient) {
        this.metadataRepository = metadataRepository;
        this.backendApiClient = backendApiClient;
    }

    @Transactional
    public void run(String token) {
        if (!isStarted.compareAndSet(false, true)) {
            return;
        }

        log.info("Checking if meeting data migration is required...");
        setupSystemSecurityContext(token);

        try {
            List<UUID> teamIds = metadataRepository.findTeamIdsWithIncompleteMetadata();
            if (teamIds.isEmpty()) {
                log.info("No meetings require migration.");
                return;
            }

            log.info("Starting migration for {} teams...", teamIds.size());

            for (UUID teamId : teamIds) {
                try {
                    TeamCardDto teamData = backendApiClient.getTeamCardById(teamId);
                    List<Meeting> corruptedMeetings = metadataRepository.findAllIncompleteByTeamCardId(teamId);

                    for (Meeting meeting : corruptedMeetings) {
                        var streamIds = teamData.getStreams()
                                .stream()
                                .map(StreamDto::getId)
                                .collect(Collectors.toSet());

                        meeting.setTeamName(teamData.getName());
                        meeting.setTrackerUsername(teamData.getUsername());
                        meeting.setStreamIds(streamIds);
                    }

                    metadataRepository.saveAll(corruptedMeetings);
                    log.info("Successfully repaired {} meetings for team: {}", corruptedMeetings.size(), teamData.getName());

                } catch (Exception e) {
                    log.error("Could not repair meetings for teamId {}: {}", teamId, e.getMessage());
                }
            }
        } finally {
            SecurityContextHolder.clearContext();
        }

        log.info("Meeting data migration completed.");
    }

    private void setupSystemSecurityContext(String tokenValue) {
        Jwt jwt = Jwt.withTokenValue(tokenValue)
                .header("alg", "none")
                .claim("sub", "migration-task")
                .build();

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}

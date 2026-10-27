package net.trackme.meetingservice.services;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.trackme.meetingservice.dao.MeetingMetadataRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.services.integration.backend.BackendApiClient;
import net.trackme.meetingservice.services.integration.backend.dto.StreamDto;
import net.trackme.meetingservice.services.integration.backend.dto.TeamCardDto;
import net.trackme.meetingservice.services.integration.sso.SsoApiClient;
import net.trackme.meetingservice.services.integration.sso.dto.UserDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


/**
 * Сервис для восстановления и аудита денормализованных данных во встречах.
 * <p>
 * Запускается асинхронно из {@code SecurityPropagationInterceptor} после первого
 * подходящего авторизованного запроса. Его токен нужен для обращения к Backend и SSO,
 * поэтому запуск при старте приложения или по таймеру без служебной авторизации не подходит.
 * Флаг завершения ограничивает синхронизацию одним успешным запуском, а при ошибке
 * разрешает повторную попытку. Восстановление дополняет обработку событий для записей
 * с неполными или устаревшими метаданными.
 * </p>
 */
@Component
@Slf4j
@Profile("!test")
public class MeetingDataBackfiller {
    private final BackendApiClient backendApiClient;
    private final SsoApiClient ssoApiClient;
    private final MeetingMetadataRepository metadataRepository;

    public MeetingDataBackfiller(
            MeetingMetadataRepository metadataRepository,
            @Lazy @Qualifier("userBackendApiClient") BackendApiClient backendApiClient,
            @Lazy SsoApiClient ssoApiClient
    ) {
        this.metadataRepository = metadataRepository;
        this.backendApiClient = backendApiClient;
        this.ssoApiClient = ssoApiClient;
    }

    @Async
    @Transactional
    public void run(String token, AtomicBoolean completionFlag) {
        if (!completionFlag.compareAndSet(false, true)) {
            return;
        }

        var originalContext = SecurityContextHolder.getContext();

        try {
            setupSystemSecurityContext(token);
            log.info("[BACKFILL] Starting global meeting data synchronization in BACKGROUND.");

            Map<String, UserDto> trackerMap = ssoApiClient.getTrackers().stream()
                    .collect(Collectors.toMap(UserDto::getUsername, u -> u, (a, b) -> a));

            Set<UUID> repairedTeamIds = repairCorruptedData(trackerMap);
            verifyAndSyncRemainingData(trackerMap, repairedTeamIds);

        } catch (Exception e) {
            log.error("[BACKFILL] Critical error, resetting flag for retry", e);
            completionFlag.set(false);
            throw e;
        } finally {
            SecurityContextHolder.setContext(originalContext);
            log.info("[BACKFILL] Global synchronization process finished.");
        }
    }

    private Set<UUID> repairCorruptedData(Map<String, UserDto> trackerMap) {
        List<UUID> corruptedIds = metadataRepository.findTeamIdsWithIncompleteMetadata();
        if (corruptedIds.isEmpty()) {
            log.info("[PHASE 1] No corrupted data (NULL fields) found.");
            return Collections.emptySet();
        }

        log.info("[PHASE 1] Found {} teams with corrupted/incomplete metadata. Starting repair...", corruptedIds.size());

        for (UUID teamId : corruptedIds) {
            try {
                TeamCardDto teamData = backendApiClient.getTeamCardById(teamId);
                List<Meeting> meetings = metadataRepository.findAllIncompleteByTeamCardId(teamId);

                syncMeetings(meetings, teamData, trackerMap, "REPAIR");
            } catch (Exception e) {
                log.error("[PHASE 1] Failed to repair team {}: {}", teamId, e.getMessage());
            }
        }
        return new HashSet<>(corruptedIds);
    }

    private void verifyAndSyncRemainingData(Map<String, UserDto> trackerMap, Set<UUID> excludedIds) {
        List<UUID> allTeamIds = metadataRepository.findAllUniqueTeamCardIds();
        List<UUID> toVerify = allTeamIds.stream()
                .filter(id -> !excludedIds.contains(id))
                .toList();

        if (toVerify.isEmpty()) {
            log.info("[PHASE 2] No additional teams to verify.");
            return;
        }

        log.info("[PHASE 2] Starting audit for remaining {} teams to ensure data consistency...", toVerify.size());

        for (UUID teamId : toVerify) {
            try {
                TeamCardDto teamData = backendApiClient.getTeamCardById(teamId);
                List<Meeting> meetings = metadataRepository.findAllByTeamCardId(teamId);

                syncMeetings(meetings, teamData, trackerMap, "AUDIT");
            } catch (Exception e) {
                log.error("[PHASE 2] Failed to audit team {}: {}", teamId, e.getMessage());
            }
        }
    }

    private void syncMeetings(List<Meeting> meetings, TeamCardDto teamData, Map<String, UserDto> trackerMap, String mode) {
        String username = teamData.getUsername();
        UserDto ssoUser = trackerMap.get(username);
        String fullName = (ssoUser != null) ? ssoUser.getFullName() : username;
        String trackerId = (ssoUser != null) ? ssoUser.getId() : null;
        Set<UUID> streamIds = teamData.getStreams().stream().map(StreamDto::getId).collect(Collectors.toSet());

        boolean isChanged = false;
        for (Meeting m : meetings) {
            if (syncMeeting(m, teamData, username, fullName, trackerId, streamIds, mode)) {
                isChanged = true;
            }
        }

        if (isChanged) {
            metadataRepository.saveAll(meetings);
            log.info("[{}] Successfully synchronized {} meetings for team: {}", mode, meetings.size(), teamData.getName());
        } else {
            log.debug("[{}] Team '{}' is already up to date.", mode, teamData.getName());
        }
    }

    private boolean syncMeeting(Meeting meeting, TeamCardDto teamData, String username,
                                String fullName, String trackerId, Set<UUID> streamIds, String mode) {
        boolean updated = false;
        if (!Objects.equals(meeting.getTeamName(), teamData.getName())) {
            log.trace("[{}] Team name mismatch for meeting {}: '{}' -> '{}'",
                    mode, meeting.getId(), meeting.getTeamName(), teamData.getName());
            meeting.setTeamName(teamData.getName());
            updated = true;
        }
        if (!Objects.equals(meeting.getTrackerUsername(), username)
                || !Objects.equals(meeting.getTrackerFullName(), fullName)) {
            log.trace("[{}] Tracker mismatch for meeting {}: '{}' -> '{}'",
                    mode, meeting.getId(), meeting.getTrackerUsername(), username);
            meeting.setTrackerUsername(username);
            meeting.setTrackerId(trackerId);
            meeting.setTrackerFullName(fullName);
            updated = true;
        }
        if (!Objects.equals(meeting.getStreamIds(), streamIds)) {
            meeting.setStreamIds(streamIds);
            updated = true;
        }
        BigDecimal oldWeight = meeting.getTeamStatusValue();
        meeting.updateTeamStatusValue();
        return oldWeight == null || oldWeight.compareTo(meeting.getTeamStatusValue()) != 0 || updated;
    }

    private void setupSystemSecurityContext(String tokenValue) {
        Jwt jwt = Jwt.withTokenValue(tokenValue)
                .header("alg", "none")
                .claim("sub", "migration-task")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}

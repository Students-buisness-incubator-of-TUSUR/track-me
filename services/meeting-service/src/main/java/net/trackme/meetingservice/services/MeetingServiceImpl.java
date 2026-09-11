package net.trackme.meetingservice.services;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import static java.util.stream.Collectors.toSet;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import net.trackme.commons.acl.AclService;
import net.trackme.meetingservice.api.dto.MeetingCreateDto;
import net.trackme.meetingservice.api.dto.MeetingDto;
import net.trackme.meetingservice.api.dto.MeetingUpdateDto;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import static net.trackme.meetingservice.entities.MeetingSpecification.meetingIdEquals;
import static net.trackme.meetingservice.entities.MeetingSpecification.teamCardIdEquals;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.entities.TeamStatus;
import net.trackme.meetingservice.mapping.MeetingMapper;
import net.trackme.meetingservice.messaging.own.MeetingCreatedEvent;
import net.trackme.meetingservice.messaging.own.MeetingDeletedEvent;
import net.trackme.meetingservice.messaging.own.MeetingEventsProducer;
import net.trackme.meetingservice.messaging.own.MeetingUpdatedEvent;
import net.trackme.meetingservice.services.exceptions.MeetingAlreadyExistsInSameDayException;
import net.trackme.meetingservice.services.exceptions.MeetingCompletedException;
import net.trackme.meetingservice.services.exceptions.MeetingEmptyImageException;
import net.trackme.meetingservice.services.exceptions.MeetingImageExtensionException;
import net.trackme.meetingservice.services.exceptions.MeetingImageNotFoundException;
import net.trackme.meetingservice.services.exceptions.MeetingImageUploadException;
import net.trackme.meetingservice.services.exceptions.MeetingLargeImageSizeException;
import net.trackme.meetingservice.services.exceptions.MeetingMIMETypeException;
import net.trackme.meetingservice.services.exceptions.MeetingNotFoundException;
import net.trackme.meetingservice.services.integration.backend.BackendApiClient;
import net.trackme.meetingservice.services.integration.backend.dto.StreamDto;
import net.trackme.meetingservice.services.integration.sso.SsoApiClient;
import net.trackme.meetingservice.services.integration.sso.dto.UserDto;

import org.springframework.security.access.AccessDeniedException;

/**
 * Реализация сервиса для управления встречами.
 */
@Slf4j
@Service
public class MeetingServiceImpl implements MeetingService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String AUTHORITY_ADMIN = "ADMIN";
    private static final String AUTHORITY_SUPER_ADMIN = "SUPER_ADMIN";
    private static final String BACKDATE_CREATE_ERROR = "Нельзя создавать встречи с датой в прошлом";
    private static final String BACKDATE_UPDATE_ERROR = "Нельзя переносить дату встречи задним числом";
    private static final String PASSIVE_TEAM_ERROR = "Трекер не может создавать встречи для пассивной команды";
    private static final String PASSIVE_TEAM_EDIT_ERROR = "Трекер не может редактировать встречи пассивной команды";

    /** Маппер для преобразования между сущностями и DTO. */
    private final MeetingMapper meetingMapper;

    /** Репозиторий для работы с встречами. */
    private final MeetingRepository meetingRepository;

    /** Сервис для управления ACL. */
    private final AclService aclService;

    /** Продюсер событий встреч. */
    private final MeetingEventsProducer meetingEventsProducer;

    /** Клиент для API бэкенда. */
    private final BackendApiClient userBackendClient;

    /** Клиент для API SSO. */
    private final SsoApiClient ssoApiClient;

    private final MutableAclService mutableAclService;

    /**
     * Конструктор сервиса встреч.
     *
     * @param meetingMapper маппер встреч
     * @param meetingRepository репозиторий встреч
     * @param aclService сервис ACL
     * @param meetingEventsProducer продюсер событий
     * @param userBackendClient клиент бэкенда
     * @param ssoApiClient клиент SSO
     */
    public MeetingServiceImpl(
            MeetingMapper meetingMapper,
            MeetingRepository meetingRepository,
            AclService aclService,
            MeetingEventsProducer meetingEventsProducer,
            @Qualifier("userBackendApiClient") BackendApiClient userBackendClient,
            SsoApiClient ssoApiClient,
            MutableAclService mutableAclService) {

        this.meetingMapper = meetingMapper;
        this.meetingRepository = meetingRepository;
        this.aclService = aclService;
        this.meetingEventsProducer = meetingEventsProducer;
        this.userBackendClient = userBackendClient;
        this.ssoApiClient = ssoApiClient;
        this.mutableAclService = mutableAclService;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"meetings-report-all", "meetings-report-page"}, allEntries = true)
    public MeetingDto createMeeting(UUID teamCardId, MeetingCreateDto createDto) {
        validateNoMeetingOnSameDay(teamCardId, createDto.startDate(), null);
        validateTeamCardNotPassive(teamCardId);
        validateNotCreatingBackdated(createDto);

        var meeting = meetingMapper.mapToEntity(createDto);
        var teamData = userBackendClient.getTeamCardById(teamCardId);
        var trackerUsername = teamData.getUsername();

        meeting.setTeamCardId(teamCardId);
        meeting.setStatus(MeetingStatus.SCHEDULED);
        meeting.setNumber("0");
        // Denormalize (Backend)
        meeting.setTeamName(teamData.getName());
        meeting.setStreamIds(teamData.getStreams().stream().map(StreamDto::getId).collect(toSet()));
        meeting.setTrackerUsername(trackerUsername);

        // Denormalize (SSO)
        if (trackerUsername != null) {
            setTrackerInfo(meeting, trackerUsername);
        }

        var savedMeeting = meetingRepository.saveAndFlush(meeting);
        renumberMeetingsAfterDeletion(teamCardId);
        recalculateTasksChain(teamCardId);

        var refreshedMeeting = meetingRepository.findById(savedMeeting.getId())
                .orElseThrow(() -> new MeetingNotFoundException(savedMeeting.getId()));

        grantAclPermissions(refreshedMeeting, trackerUsername);

        sendMeetingCreatedEvent(refreshedMeeting);

        return enrichWithRoomLink(meetingMapper.mapToDto(refreshedMeeting), teamCardId);
    }

    @Override
    public Page<MeetingDto> getMeetings(UUID teamCardId, Pageable pageable) {
        var meetings = meetingRepository.findAll(teamCardIdEquals(teamCardId), pageable);
        var roomLink = fetchRoomLink(teamCardId);
        return meetings.map(m -> withRoomLink(meetingMapper.mapToDto(m), roomLink));
    }

    @Override
    @Transactional
    @PreAuthorize(
            "hasPermission(#meetingId,'net.trackme.meetingservice.entities.Meeting', 'WRITE') "
                    + "or hasRole('ADMIN')")
    @CacheEvict(value = {"meetings-report-all", "meetings-report-page"}, allEntries = true)
    public MeetingDto updateMeeting(UUID meetingId, UUID teamCardId, MeetingUpdateDto updateDto) {
        log.info("updateMeeting called by user: {}",
         SecurityContextHolder.getContext().getAuthentication().getName());
        log.info("Authorities: {}",
         SecurityContextHolder.getContext().getAuthentication().getAuthorities());
        
        var meeting = findMeeting(meetingId, teamCardId);
        validateTeamCardNotPassiveForEdit(teamCardId);
        validateNotCompletedForNonSuperAdmin(meeting, meetingId, teamCardId);
        validateNotBackdating(meeting, updateDto);

        OffsetDateTime oldStartDate = meeting.getStartDate();
        boolean dateChanged = isDateChanged(updateDto, oldStartDate);

        if (dateChanged) {
            validateNoMeetingOnSameDay(teamCardId, updateDto.startDate(), meetingId);
        }

        var oldStatus = meeting.getStatus();
        var oldTeamStatus = meeting.getTeamStatus();

        // Сохраняем старое значение ДО обновления
        String oldTasksNext = meeting.getTasksNextMeeting();

        meetingMapper.updateEntityFromDto(updateDto, meeting);

        handleCompletedAsNotHappened(updateDto, meeting);
        handleTasksNextManualFlag(updateDto, oldTasksNext, meeting);

        var savedMeeting = saveAndRenumberIfDateChanged(meeting, dateChanged, teamCardId, meetingId);
        recalculateTasksChain(teamCardId);

        sendUpdateEventIfStatusChanged(savedMeeting, oldStatus, oldTeamStatus);

        return enrichWithRoomLink(meetingMapper.mapToDto(savedMeeting), teamCardId);
    }

    @Override
    @Transactional
    @PreAuthorize(
            "hasPermission(#meetingId,'net.trackme.meetingservice.entities.Meeting', 'WRITE') "
                    + "or hasRole('ADMIN')")
    @CacheEvict(value = {"meetings-report-all", "meetings-report-page"}, allEntries = true)
    public void deleteMeeting(UUID meetingId) {
        log.debug("Deleting meeting: {}", meetingId);
        
        var meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(meetingId));

        logAdminDeletion(meetingId, meeting);

        UUID teamCardId = meeting.getTeamCardId();

        meetingRepository.delete(meeting);
        aclService.deleteAcl(meeting);

        renumberMeetingsAfterDeletion(teamCardId);
        recalculateTasksChain(teamCardId);
        meetingRepository.flush();

        sendMeetingDeletedEvent(meetingId, teamCardId, meeting);

        log.debug("Meeting {} deleted successfully", meetingId);
    }

    @Override
    @Transactional
    @PreAuthorize(
            "hasPermission(#meetingId,'net.trackme.meetingservice.entities.Meeting', 'WRITE') "
                    + "or hasRole('ADMIN')")
    public void addMeetingImage(UUID meetingId, MultipartFile file) {
        validateImageFile(file);

        var meeting = meetingRepository.getReferenceById(meetingId);
        try {
            meeting.setImageBytes(file.getBytes());
            meetingRepository.save(meeting);
        } catch (IOException e) {
            throw new MeetingImageUploadException(meetingId, e);
        }
    }

    @Override
    @PreAuthorize(
            "hasPermission(#meetingId,'net.trackme.meetingservice.entities.Meeting', 'READ') "
                    + "or hasRole('ADMIN')")
    public Resource getMeetingImage(UUID meetingId) {
        var meeting = getMeeting(meetingId);
        if (meeting.getImageBytes() == null) {
            throw new MeetingImageNotFoundException(meetingId);
        }
        return new ByteArrayResource(meeting.getImageBytes());
    }

    /**
     * Обновление встречи администратором.
     * ADMIN может менять все поля кроме status и teamStatus.
     * ADMIN может менять startDate (дату встречи), но не задним числом.
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = {"meetings-report-all", "meetings-report-page"}, allEntries = true)
    public MeetingDto updateByAdmin(UUID meetingId, UUID teamCardId, MeetingUpdateDto updateDto) {
        log.info("updateByAdmin called by user: {}", 
            SecurityContextHolder.getContext().getAuthentication().getName());
        
        validateCurrentUserIsAdmin();
        validateAdminNotChangingStatus(updateDto);

        var meeting = findMeeting(meetingId, teamCardId);
        validateNotBackdating(meeting, updateDto);

        var oldStatus = meeting.getStatus();
        var oldTeamStatus = meeting.getTeamStatus();
        String oldTasksNext = meeting.getTasksNextMeeting();
        OffsetDateTime oldStartDate = meeting.getStartDate();

        boolean dateChanged = isDateChanged(updateDto, oldStartDate);

        if (dateChanged) {
            validateNoMeetingOnSameDay(teamCardId, updateDto.startDate(), meetingId);
        }

        meetingMapper.updateEntityFromDtoForAdmin(updateDto, meeting);
        handleTasksNextManualFlag(updateDto, oldTasksNext, meeting);

        var savedMeeting = saveAndRenumberIfDateChanged(meeting, dateChanged, teamCardId, meetingId);
        recalculateTasksChain(teamCardId);
        
        savedMeeting = refreshMeeting(meetingId);

        sendUpdateEventIfStatusChanged(savedMeeting, oldStatus, oldTeamStatus);

        log.info("Admin {} updated meeting {}", getCurrentUsername(), meetingId);
        
        return enrichWithRoomLink(meetingMapper.mapToDto(savedMeeting), teamCardId);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"meetings-report-all", "meetings-report-page"}, allEntries = true)
    public MeetingDto updateBySuperAdmin(UUID meetingId, MeetingUpdateDto updateDto) {
        validateCurrentUserIsSuperAdmin();
        
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(meetingId));

        if (!meeting.getStatus().isEditableBySuperAdmin()) {
            throw new IllegalStateException(
                String.format(
                    "Невозможно редактировать встречу со статусом '%s'. Разрешены только: '%s' и '%s'.",
                    meeting.getStatus().getDescription(),
                    MeetingStatus.FINALLY_COMPLETED.getDescription(),
                    MeetingStatus.COMPLETED_AS_NOT_HAPPENED.getDescription())
            );
        }
        
        OffsetDateTime oldStartDate = meeting.getStartDate();
        boolean dateChanged = isDateChanged(updateDto, oldStartDate);
        
        var oldStatus = meeting.getStatus();
        var oldTeamStatus = meeting.getTeamStatus();
        
        meetingMapper.updateEntityFromDto(updateDto, meeting);
        handleCompletedAsNotHappened(updateDto, meeting);
        
        Meeting savedMeeting = meetingRepository.save(meeting);
        
        if (dateChanged) {
            UUID teamCardId = meeting.getTeamCardId();
            renumberMeetingsAfterDateChange(teamCardId);
            meetingRepository.flush();
            savedMeeting = refreshMeeting(meetingId);
            recalculateTasksChain(teamCardId);
        }
        
        sendUpdateEventIfStatusChanged(savedMeeting, oldStatus, oldTeamStatus);
        
        log.info("Super admin {} updated meeting {}", getCurrentUsername(), meetingId);
        
        return meetingMapper.mapToDto(savedMeeting);
    }


    private void validateTeamCardNotPassive(UUID teamCardId) {
        var teamData = userBackendClient.getTeamCardById(teamCardId);
        if (teamData.getPassive() != null && teamData.getPassive() && !isCurrentUserAdminOrSuperAdmin()) {
            throw new IllegalStateException(PASSIVE_TEAM_ERROR);
        }
    }

    private void validateTeamCardNotPassiveForEdit(UUID teamCardId) {
        var teamData = userBackendClient.getTeamCardById(teamCardId);
        if (teamData.getPassive() != null && teamData.getPassive() && !isCurrentUserAdminOrSuperAdmin()) {
            throw new IllegalStateException(PASSIVE_TEAM_EDIT_ERROR);
        }
    }

    /**
     * TRACKER и ADMIN не могут создавать встречи задним числом.
     * Только SUPER_ADMIN может.
     */
    private void validateNotCreatingBackdated(MeetingCreateDto createDto) {
        if (isCurrentUserSuperAdmin()) return;
        
        if (createDto.startDate() != null 
                && createDto.startDate().isBefore(OffsetDateTime.now())) {
            throw new AccessDeniedException(BACKDATE_CREATE_ERROR);
        }
    }

    /**
     * TRACKER и ADMIN не могут переносить дату встречи задним числом.
     * Только SUPER_ADMIN может.
     */
    private void validateNotBackdating(Meeting meeting, MeetingUpdateDto updateDto) {
        if (updateDto.startDate() == null) return;
        if (isCurrentUserSuperAdmin()) return;
        
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime newDate = updateDto.startDate();
        OffsetDateTime oldDate = meeting.getStartDate();
        
        // Если новая дата в прошлом и отличается от старой — запрещено
        if (newDate.isBefore(now) && !newDate.equals(oldDate)) {
            throw new AccessDeniedException(BACKDATE_UPDATE_ERROR);
        }
    }

    private void validateNotCompletedForNonSuperAdmin(Meeting meeting, UUID meetingId, UUID teamCardId) {
        if (!isCurrentUserSuperAdmin() && MeetingStatus.COMPLETED_STATUSES.contains(meeting.getStatus())) {
            throw new MeetingCompletedException(meetingId, teamCardId);
        }
    }

    private void validateCurrentUserIsAdmin() {
        if (!isCurrentUserOnlyAdmin()) {
            throw new AccessDeniedException("Только администратор может использовать этот метод");
        }
    }

    private void validateCurrentUserIsSuperAdmin() {
        var authentication = getAuthentication();
        if (authentication == null) {
            throw new AccessDeniedException("Пользователь не аутентифицирован");
        }
        
        if (!isCurrentUserSuperAdmin()) {
            throw new AccessDeniedException(
                "Только суперадминистратор может редактировать встречи со статусами " +
                "'Окончательно завершена' или 'Завершена как не состоявшаяся'"
            );
        }
    }

    private void validateAdminNotChangingStatus(MeetingUpdateDto updateDto) {
        if (updateDto.status() != null) {
            throw new AccessDeniedException(
                "Администратор не может изменять основной статус встречи (status)"
            );
        }
        
        if (updateDto.teamStatus() != null) {
            throw new AccessDeniedException(
                "Администратор не может изменять статус команды (teamStatus)"
            );
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new MeetingEmptyImageException();
        }

        if (file.getSize() > MeetingService.MAX_FILE_SIZE) {
            throw new MeetingLargeImageSizeException(file.getSize());
        }

        String contentType = file.getContentType();
        if (!MediaType.IMAGE_PNG_VALUE.equals(contentType)
                && !MediaType.IMAGE_JPEG_VALUE.equals(contentType)) {
            throw new MeetingMIMETypeException(contentType);
        }

        String originalName = file.getOriginalFilename();
        if (originalName != null) {
            String ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
            if (!ext.equals("png") && !ext.equals("jpg") && !ext.equals("jpeg")) {
                throw new MeetingImageExtensionException(ext);
            }
        }
    }

    /**
     * Проверяет, является ли текущий пользователь ADMIN или SUPER_ADMIN.
     * Смотрит на ВСЕ роли, а не только первую.
     */
    private boolean isCurrentUserAdminOrSuperAdmin() {
        var authentication = getAuthentication();
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> {
                    String auth = a.getAuthority();
                    return ROLE_ADMIN.equals(auth)
                            || AUTHORITY_ADMIN.equals(auth)
                            || ROLE_SUPER_ADMIN.equals(auth)
                            || AUTHORITY_SUPER_ADMIN.equals(auth);
                });
    }

    private boolean isCurrentUserOnlyAdmin() {
        var authentication = getAuthentication();
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> ROLE_ADMIN.equals(auth.getAuthority())
                              || AUTHORITY_ADMIN.equals(auth.getAuthority()));
    }

    private boolean isCurrentUserSuperAdmin() {
        var authentication = getAuthentication();
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> ROLE_SUPER_ADMIN.equals(auth.getAuthority())
                              || AUTHORITY_SUPER_ADMIN.equals(auth.getAuthority()));
    }

    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private String getCurrentUsername() {
        var authentication = getAuthentication();
        return authentication != null ? authentication.getName() : "unknown";
    }

    private void setTrackerInfo(Meeting meeting, String trackerUsername) {
        var tracker = ssoApiClient.getTrackers().stream()
                .filter(u -> trackerUsername.equalsIgnoreCase(u.getUsername()))
                .findFirst();

        if (tracker.isPresent()) {
            UserDto user = tracker.get();
            meeting.setTrackerId(user.getId());
            meeting.setTrackerFullName(user.getFullName());
        } else {
            meeting.setTrackerFullName(trackerUsername);
            log.warn("Tracker with username {} not found in SSO during meeting creation", 
                    trackerUsername);
        }
    }

    private void grantAclPermissions(Meeting meeting, String trackerUsername) {
        var creatorUsername = getCurrentUsername();
        aclService.createAclForUser(meeting, creatorUsername);

        if (trackerUsername != null && !trackerUsername.isBlank()
                && !trackerUsername.equalsIgnoreCase(creatorUsername)) {
            try {
                ObjectIdentity oid = new ObjectIdentityImpl(meeting);
                MutableAcl acl = (MutableAcl) mutableAclService.readAclById(oid);
                aclService.addPermissionsToUser(acl, trackerUsername, List.of(
                        BasePermission.READ,
                        BasePermission.WRITE
                ));
                log.info("Tracker {} granted READ+WRITE on meeting {}",
                        trackerUsername, meeting.getId());
            } catch (Exception e) {
                log.error("Failed to grant ACL to tracker {} for meeting {}: {}. "
                        + "Tracker will not be able to edit this meeting until ACL is fixed manually.",
                        trackerUsername, meeting.getId(), e.getMessage());
            }
        }
    }

    private Meeting findMeeting(UUID meetingId, UUID teamCardId) {
        return meetingRepository.findOne(teamCardIdEquals(teamCardId)
                        .and(meetingIdEquals(meetingId)))
                .orElseThrow(() -> new MeetingNotFoundException(meetingId, teamCardId));
    }

    private boolean isDateChanged(MeetingUpdateDto updateDto, OffsetDateTime oldStartDate) {
        return updateDto.startDate() != null
                && !updateDto.startDate().equals(oldStartDate);
    }

    private void handleCompletedAsNotHappened(MeetingUpdateDto updateDto, Meeting meeting) {
        if (updateDto.status() == MeetingStatus.COMPLETED_AS_NOT_HAPPENED) {
            meeting.setTeamStatus(null);
        }
    }

    private void handleTasksNextManualFlag(MeetingUpdateDto updateDto, String oldTasksNext, Meeting meeting) {
        if (updateDto.tasksNextMeeting() != null) {
            String newValue = updateDto.tasksNextMeeting();
            if (!newValue.equals(oldTasksNext)) {
                meeting.setTasksNextManuallySet(true);
            }
            if (newValue.isBlank()) {
                meeting.setTasksNextManuallySet(false);
            }
        }
    }

    private Meeting saveAndRenumberIfDateChanged(Meeting meeting, boolean dateChanged, 
                                                  UUID teamCardId, UUID meetingId) {
        var savedMeeting = meetingRepository.saveAndFlush(meeting);

        if (dateChanged) {
            renumberMeetingsAfterDateChange(teamCardId);
            meetingRepository.flush();
            return refreshMeeting(meetingId);
        }
        
        return savedMeeting;
    }

    private Meeting refreshMeeting(UUID meetingId) {
        return meetingRepository.findById(meetingId)
            .orElseThrow(() -> new MeetingNotFoundException(meetingId));
    }

    private void sendUpdateEventIfStatusChanged(Meeting meeting, MeetingStatus oldStatus, 
                                                 TeamStatus oldTeamStatus) {
        if (oldStatus != meeting.getStatus()
                || !Objects.equals(oldTeamStatus, meeting.getTeamStatus())) {
            sendMeetingUpdatedEvent(meeting, oldStatus);
        }
    }

    private void logAdminDeletion(UUID meetingId, Meeting meeting) {
        if (isCurrentUserOnlyAdmin()) {
            log.warn("ADMIN {} is deleting meeting {} (teamCardId={}, startDate={}, status={})", 
                getCurrentUsername(), meetingId, meeting.getTeamCardId(), 
                meeting.getStartDate(), meeting.getStatus());
        }
    }

    /**
     * Пересчитывает номера встреч после удаления или изменения даты.
     * Встречи сортируются по дате (от ранних к поздним) и получают новые номера.
     *
     * @param teamCardId идентификатор карточки команды
     */
    private void renumberMeetingsAfterDeletion(UUID teamCardId) {
        var meetings = meetingRepository.findAll(
                teamCardIdEquals(teamCardId),
                Sort.by(Sort.Direction.ASC, "startDate")
        );

        int number = 1;
        for (Meeting m : meetings) {
            m.setNumber(String.valueOf(number));
            number++;
        }

        meetingRepository.saveAllAndFlush(meetings);
        log.debug("Renumbered {} meetings for team card {}", meetings.size(), teamCardId);
    }

    /**
     * Перенумеровывает встречи после изменения даты.
     *
     * @param teamCardId идентификатор карточки команды
     */
    private void renumberMeetingsAfterDateChange(UUID teamCardId) {
        renumberMeetingsAfterDeletion(teamCardId);
    }

    /**
     * Проверяет отсутствие встречи для карточки команды в указанный день.
     *
     * @param teamCardId идентификатор карточки команды
     * @param startDate дата начала встречи
     * @param excludeId ID встречи для исключения из проверки (может быть null)
     */
    private void validateNoMeetingOnSameDay(UUID teamCardId, OffsetDateTime startDate, UUID excludeId) {
        var date = startDate.toLocalDate();
        var from = date.atStartOfDay().atOffset(startDate.getOffset());
        var to = date.plusDays(1).atStartOfDay().atOffset(startDate.getOffset());

        boolean existsOnSameDay = excludeId == null
                ? meetingRepository.existsByTeamCardIdAndStartDateGreaterThanEqualAndStartDateLessThan(
                    teamCardId, from, to)
                : meetingRepository.existsByTeamCardIdAndStartDateGreaterThanEqualAndStartDateLessThanAndIdNot(
                    teamCardId, from, to, excludeId);

        if (existsOnSameDay) {
            throw new MeetingAlreadyExistsInSameDayException(
                    "В этот день уже запланирована встреча для данной команды.");
        }
    }

    /**
     * Получает встречу по ID.
     *
     * @param meetingId идентификатор встречи
     * @return сущность встречи
     * @throws MeetingNotFoundException если встреча не найдена
     */
    private Meeting getMeeting(UUID meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(meetingId));
    }

    /**
     * Отправляет событие об обновлении встречи.
     *
     * @param meeting обновленная встреча
     * @param oldStatus предыдущий статус
     */
    private void sendMeetingUpdatedEvent(Meeting meeting, MeetingStatus oldStatus) {
        var event = MeetingUpdatedEvent.builder()
                .meetingId(meeting.getId())
                .newStatus(meeting.getStatus())
                .oldStatus(oldStatus)
                .teamStatus(meeting.getTeamStatus())
                .teamCardId(meeting.getTeamCardId())
                .teamGrade(meeting.getTeamStatusValue() != null ? meeting.getTeamStatusValue().doubleValue() : 0)
                .build();

        meetingEventsProducer.sendMeetingUpdatedEvent(event);
    }

    /**
     * Отправляет событие о создании встречи.
     *
     * @param meeting созданная встреча
     */
    private void sendMeetingCreatedEvent(Meeting meeting) {
        var event = MeetingCreatedEvent.builder()
                .meetingId(meeting.getId())
                .teamCardId(meeting.getTeamCardId())
                .build();
        meetingEventsProducer.sendMeetingCreatedEvent(event);
    }

    /**
     * Отправляет событие об удалении встречи.
     *
     * @param meetingId идентификатор встречи
     * @param teamCardId идентификатор карточки команды
     * @param meeting сущность встречи
     */
    private void sendMeetingDeletedEvent(UUID meetingId, UUID teamCardId, Meeting meeting) {
        var event = MeetingDeletedEvent.builder()
                .meetingId(meetingId)
                .teamCardId(teamCardId)
                .startDate(meeting.getStartDate())
                .status(meeting.getStatus())
                .build();
        meetingEventsProducer.sendMeetingDeletedEvent(event);
    }

    /**
     * Получает ссылку на комнату встречи.
     *
     * @param teamCardId идентификатор карточки команды
     * @return ссылка на комнату или null
     */
    private String fetchRoomLink(UUID teamCardId) {
        try {
            var teamCard = userBackendClient.getTeamCardById(teamCardId);
            return teamCard != null ? teamCard.getMeetingRoomLink() : null;
        } catch (Exception e) {
            log.warn("Не удалось получить roomLink для teamCardId={}: {} | cause: {}",
                    teamCardId, e.getMessage(),
                    e.getCause() != null ? e.getCause().getMessage() : "no cause");
            return null;
        }
    }

    /**
     * Обогащает DTO встречи ссылкой на комнату.
     *
     * @param dto DTO встречи
     * @param teamCardId идентификатор карточки команды
     * @return обогащенный DTO
     */
    private MeetingDto enrichWithRoomLink(MeetingDto dto, UUID teamCardId) {
        return withRoomLink(dto, fetchRoomLink(teamCardId));
    }

    /**
     * Добавляет ссылку на комнату в DTO.
     *
     * @param dto DTO встречи
     * @param roomLink ссылка на комнату
     * @return DTO с добавленной ссылкой
     */
    private MeetingDto withRoomLink(MeetingDto dto, String roomLink) {
        return new MeetingDto(
                dto.id(),
                dto.recordLink(),
                roomLink,
                dto.number(),
                dto.startDate(),
                dto.teamStatus(),
                dto.status(),
                dto.teamCardId(),
                dto.tasksCurrentMeeting(),
                dto.tasksNextMeeting()
        );
    }

    /**
     * Пересчитывает цепочку задач для всех встреч команды.
     * tasksCurrentMeeting предыдущей встречи → tasksNextMeeting следующей встречи.
     *
     * @param teamCardId идентификатор карточки команды
     */
    private void recalculateTasksChain(UUID teamCardId) {
        var meetings = meetingRepository.findAll(
            teamCardIdEquals(teamCardId),
            Sort.by(Sort.Direction.ASC, "startDate")
        );
        
        if (meetings.isEmpty()) {
            return;
        }
        
        clearFirstMeetingIfNeeded(meetings.get(0));
        
        for (int i = 1; i < meetings.size(); i++) {
            updateTasksNextIfNotManual(meetings.get(i), meetings, i);
        }
        
        meetingRepository.saveAllAndFlush(meetings);
    }

    private void clearFirstMeetingIfNeeded(Meeting first) {
        if (!first.isTasksNextManuallySet()) {
            first.setTasksNextMeeting(null);
        }
    }

    private void updateTasksNextIfNotManual(Meeting current, List<Meeting> allMeetings, int currentIndex) {
        if (current.isTasksNextManuallySet()) {
            return;
        }
        
        String newValue = findTasksFromLastHappenedMeeting(allMeetings, currentIndex);
        current.setTasksNextMeeting(newValue);
    }

    private String findTasksFromLastHappenedMeeting(List<Meeting> meetings, int currentIndex) {
        for (int j = currentIndex - 1; j >= 0; j--) {
            Meeting previous = meetings.get(j);
            if (previous.getStatus() != MeetingStatus.COMPLETED_AS_NOT_HAPPENED) {
                return getNonBlankTasksCurrent(previous);
            }
        }
        return null;
    }

    private String getNonBlankTasksCurrent(Meeting meeting) {
        String tasks = meeting.getTasksCurrentMeeting();
        return (tasks != null && !tasks.isBlank()) ? tasks : null;
    }
}

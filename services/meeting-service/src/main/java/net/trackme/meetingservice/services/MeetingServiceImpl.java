package net.trackme.meetingservice.services;

import lombok.extern.slf4j.Slf4j;
import net.trackme.commons.acl.AclService;
import net.trackme.meetingservice.api.dto.MeetingCreateDto;
import net.trackme.meetingservice.api.dto.MeetingDto;
import net.trackme.meetingservice.api.dto.MeetingUpdateDto;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.messaging.own.MeetingCreatedEvent;
import net.trackme.meetingservice.messaging.own.MeetingUpdatedEvent;
import net.trackme.meetingservice.mapping.MeetingMapper;
import net.trackme.meetingservice.services.exceptions.*;
import net.trackme.meetingservice.services.integration.backend.BackendApiClient;
import net.trackme.meetingservice.services.integration.backend.dto.StreamDto;
import net.trackme.meetingservice.services.integration.sso.SsoApiClient;
import net.trackme.meetingservice.services.integration.sso.dto.UserDto;
import net.trackme.meetingservice.messaging.own.MeetingEventsProducer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.UUID;

import static java.util.stream.Collectors.toSet;
import static net.trackme.meetingservice.entities.MeetingSpecification.meetingIdEquals;
import static net.trackme.meetingservice.entities.MeetingSpecification.teamCardIdEquals;

@Slf4j
@Service
public class MeetingServiceImpl implements MeetingService {

    private final MeetingMapper meetingMapper;

    private final MeetingRepository meetingRepository;

    private final AclService aclService;

    private final MeetingEventsProducer meetingEventsProducer;

    private final BackendApiClient userBackendClient;

    private final SsoApiClient ssoApiClient;

    public MeetingServiceImpl(
            MeetingMapper meetingMapper,
            MeetingRepository meetingRepository,
            AclService aclService,
            MeetingEventsProducer meetingEventsProducer,
            @Qualifier("userBackendApiClient") BackendApiClient userBackendClient,
            SsoApiClient ssoApiClient) {

        this.meetingMapper = meetingMapper;
        this.meetingRepository = meetingRepository;
        this.aclService = aclService;
        this.meetingEventsProducer = meetingEventsProducer;
        this.userBackendClient = userBackendClient;
        this.ssoApiClient = ssoApiClient;
    }

    @Override
    @Transactional
    public MeetingDto createMeeting(UUID teamCardId, MeetingCreateDto createDto) {
        validateNoMeetingOnSameDay(teamCardId, createDto.startDate(), null);

        var meeting = meetingMapper.mapToEntity(createDto);
        var teamData = userBackendClient.getTeamCardById(teamCardId);
        var trackerUsername = teamData.getUsername();

        meeting.setTeamCardId(teamCardId);
        meeting.setStatus(MeetingStatus.SCHEDULED);

        // Denormalize (Backend)
        meeting.setTeamName(teamData.getName());
        meeting.setStreamIds(teamData.getStreams().stream().map(StreamDto::getId).collect(toSet()));
        meeting.setTrackerUsername(trackerUsername);

        // Denormalize (SSO)
        if (trackerUsername != null) {
            var tracker = ssoApiClient.getTrackers().stream()
                    .filter(u -> trackerUsername.equalsIgnoreCase(u.getUsername()))
                    .findFirst();

            if (tracker.isPresent()) {
                UserDto user = tracker.get();
                meeting.setTrackerId(user.getId());
                meeting.setTrackerFullName(user.getFullName());
            } else {
                meeting.setTrackerFullName(trackerUsername);
                log.warn("Tracker with username {} not found in SSO during meeting creation", trackerUsername);
            }
        }

        meeting = meetingRepository.save(meeting);
        var username = SecurityContextHolder.getContext().getAuthentication().getName();

        aclService.createAclForUser(meeting, username);
        sendMeetingCreatedEvent(meeting);

        return enrichWithRoomLink(meetingMapper.mapToDto(meeting), teamCardId);
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
            "hasPermission(#meetingId,'net.trackme.meetingservice.entities.Meeting', 'WRITE') or hasRole('ADMIN')")
    public MeetingDto updateMeeting(UUID meetingId, UUID teamCardId, MeetingUpdateDto updateDto) {
        var meeting = meetingRepository.findOne(teamCardIdEquals(teamCardId)
                        .and(meetingIdEquals(meetingId)))
                .orElseThrow(() -> new MeetingNotFoundException(meetingId, teamCardId));

        if (MeetingStatus.COMPLETED_STATUSES.contains(meeting.getStatus())) {
            throw new MeetingCompletedException(meetingId, teamCardId);
        }

        if (updateDto.startDate() != null) {
            validateNoMeetingOnSameDay(teamCardId, updateDto.startDate(), meetingId);
        }

        var oldStatus = meeting.getStatus();
        meetingMapper.updateEntityFromDto(updateDto, meeting);
        meeting = meetingRepository.save(meeting);

        if (oldStatus != meeting.getStatus()) {
            sendMeetingUpdatedEvent(meeting, oldStatus);
        }
        return enrichWithRoomLink(meetingMapper.mapToDto(meeting), teamCardId);
    }

    @Override
    @Transactional
    @PreAuthorize(
            "hasPermission(#meetingId,'net.trackme.meetingservice.entities.Meeting', 'READ') or hasRole('ADMIN')")
    public void deleteMeeting(UUID meetingId) {
        var meeting = meetingRepository.getReferenceById(meetingId);
        meetingRepository.delete(meeting);
        aclService.deleteAcl(meeting);
    }

    @Override
    @Transactional
    @PreAuthorize(
            "hasPermission(#meetingId,'net.trackme.meetingservice.entities.Meeting', 'WRITE') or hasRole('ADMIN')")
    public void addMeetingImage(UUID meetingId, MultipartFile file) {
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

        var meeting = meetingRepository.getReferenceById(meetingId);
        try {
            meeting.setImageBytes(file.getBytes());
            meetingRepository.save(meeting);
        } catch (Exception e) {
            throw new MeetingImageUploadException(meetingId, e);
        }
    }

    @Override
    @PreAuthorize(
            "hasPermission(#meetingId,'net.trackme.meetingservice.entities.Meeting', 'READ') or hasRole('ADMIN')")
    public Resource getMeetingImage(UUID meetingId) {
        var meeting = getMeeting(meetingId);
        if (meeting.getImageBytes() == null) {
            throw new MeetingImageNotFoundException(meetingId);
        }
        return new ByteArrayResource(meeting.getImageBytes());
    }

    /**
     * Проверяет отсутствие встречи для карточки команды в указанный день.
     *
     * @param teamCardId идентификатор карточки команды
     * @param startDate  дата и время встречи
     * @param excludeId  идентификатор встречи для исключения из проверки,
     *                   {@code null} при создании новой встречи
     * @throws MeetingAlreadyExistsInSameDayException если встреча на этот день уже существует
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

    private Meeting getMeeting(UUID meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(meetingId));
    }

    private void sendMeetingUpdatedEvent(Meeting meeting, MeetingStatus oldStatus) {
        var event = MeetingUpdatedEvent.builder()
                .meetingId(meeting.getId())
                .newStatus(meeting.getStatus())
                .oldStatus(oldStatus)
                .teamStatus(meeting.getTeamStatus())
                .teamCardId(meeting.getTeamCardId())
                .teamGrade(
                        meeting.getTeamStatus() == null
                                ? 0
                                : meeting.getTeamStatus().getValue()
                )
                .build();

        meetingEventsProducer.sendMeetingUpdatedEvent(event);
    }

    private void sendMeetingCreatedEvent(Meeting meeting) {
        var event = MeetingCreatedEvent.builder()
                .meetingId(meeting.getId())
                .teamCardId(meeting.getTeamCardId())
                .build();
        meetingEventsProducer.sendMeetingCreatedEvent(event);
    }

    private String fetchRoomLink(UUID teamCardId) {
        try {
            var teamCard = userBackendClient.getTeamCardById(teamCardId);

            return teamCard != null
                    ? teamCard.getMeetingRoomLink()
                    : null;

        } catch (Exception e) {
            log.warn("Не удалось получить roomLink для teamCardId={}: {} | cause: {}",
                    teamCardId, e.getMessage(),
                    e.getCause() != null ? e.getCause().getMessage() : "no cause");
            return null;
        }
    }

    private MeetingDto enrichWithRoomLink(MeetingDto dto, UUID teamCardId) {
        return withRoomLink(dto, fetchRoomLink(teamCardId));
    }

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
}

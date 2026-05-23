package net.trackme.meetingservice.services;

import net.trackme.meetingservice.api.dto.MeetingCreateDto;
import net.trackme.meetingservice.api.dto.MeetingUpdateDto;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.mapping.MeetingMapper;
import net.trackme.meetingservice.services.integration.backend.BackendApiClient;
import net.trackme.meetingservice.services.integration.backend.dto.TeamCardDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingPassiveFlagTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingMapper meetingMapper;

    @Mock(name = "userBackendApiClient")
    private BackendApiClient userBackendClient;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private MeetingServiceImpl meetingService;

    private UUID teamCardId;
    private OffsetDateTime now;
    private UUID meetingId;

    @BeforeEach
    void setUp() {
        teamCardId = UUID.randomUUID();
        meetingId = UUID.randomUUID();
        now = OffsetDateTime.now();
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createMeeting_whenTeamIsPassiveAndUserIsNotAdmin_shouldThrowException() {
        // Arrange
        MeetingCreateDto createDto = MeetingCreateDto.builder()
                .startDate(now)
                .build();

        TeamCardDto teamCardDto = new TeamCardDto();
        teamCardDto.setId(teamCardId);
        teamCardDto.setPassive(true);
        teamCardDto.setUsername("tracker");
        teamCardDto.setName("Test Team");
        teamCardDto.setStreams(Collections.emptyList());

        when(userBackendClient.getTeamCardById(teamCardId)).thenReturn(teamCardDto);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn(Collections.emptySet());

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            meetingService.createMeeting(teamCardId, createDto);
        });

        assertEquals("Трекер не может создавать встречи для пассивной команды", exception.getMessage());
        verify(meetingRepository, never()).save(any());
    }



    // ТЕСТ 3: обновление - пассивная команда + НЕ АДМИН = ошибка
    @Test
    void updateMeeting_passiveTeam_notAdmin_throwsException() {
        MeetingUpdateDto updateDto = MeetingUpdateDto.builder()
                .tasksCurrentMeeting("Updated tasks")
                .build();

        TeamCardDto teamCardDto = new TeamCardDto();
        teamCardDto.setId(teamCardId);
        teamCardDto.setPassive(true);

        Meeting existingMeeting = new Meeting();
        existingMeeting.setId(meetingId);
        existingMeeting.setTeamCardId(teamCardId);
        existingMeeting.setStatus(MeetingStatus.SCHEDULED);

        when(userBackendClient.getTeamCardById(teamCardId)).thenReturn(teamCardDto);
        when(meetingRepository.findOne(any(Specification.class))).thenReturn(Optional.of(existingMeeting));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn(Collections.emptySet());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            meetingService.updateMeeting(meetingId, teamCardId, updateDto);
        });

        assertEquals("Трекер не может редактировать встречи пассивной команды", exception.getMessage());
    }
}
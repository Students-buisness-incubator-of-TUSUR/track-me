package net.trackme.meetingservice.services;

import net.trackme.meetingservice.api.dto.MeetingUpdateDto;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.mapping.MeetingMapper;
import net.trackme.meetingservice.services.exceptions.MeetingCompletedException;
import net.trackme.meetingservice.services.integration.backend.BackendApiClient;
import net.trackme.meetingservice.services.integration.sso.SsoApiClient;
import net.trackme.commons.acl.AclService;
import net.trackme.meetingservice.messaging.own.MeetingEventsProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingServiceImplSuperAdminTest {

    @Mock
    private MeetingRepository meetingRepository;
    
    @Mock
    private MeetingMapper meetingMapper;
    
    @Mock
    private AclService aclService;
    
    @Mock
    private MeetingEventsProducer meetingEventsProducer;
    
    @Mock
    private BackendApiClient userBackendClient;
    
    @Mock
    private SsoApiClient ssoApiClient;
    
    @InjectMocks
    private MeetingServiceImpl meetingService;
    
    private UUID meetingId;
    private UUID teamCardId;
    private Meeting meeting;
    private MeetingUpdateDto updateDto;
    
    @BeforeEach
    void setUp() {
        meetingId = UUID.randomUUID();
        teamCardId = UUID.randomUUID();
        
        meeting = new Meeting();
        meeting.setId(meetingId);
        meeting.setTeamCardId(teamCardId);
        meeting.setStatus(MeetingStatus.COMPLETED);
        meeting.setStartDate(OffsetDateTime.now());
        
        updateDto = MeetingUpdateDto.builder()
                .startDate(OffsetDateTime.now().plusDays(1))
                .recordLink("https://new-record-link.com")
                .tasksCurrentMeeting("Новые задачи")
                .build();
    }
    
    private void setupSecurityContext(String role) {
        Authentication auth = mock(Authentication.class);
        when(auth.getAuthorities()).thenAnswer(invocation -> List.of(new SimpleGrantedAuthority(role)));
        when(auth.getName()).thenReturn("testuser");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }
    
    @Test
    void superAdminCanEditCompletedMeeting() {
        setupSecurityContext("ROLE_SUPER_ADMIN");
        when(meetingRepository.findOne(any())).thenReturn(Optional.of(meeting));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);
        
        assertDoesNotThrow(() -> meetingService.updateMeeting(meetingId, teamCardId, updateDto));
        verify(meetingRepository, times(1)).save(any(Meeting.class));
    }
    
    @Test
    void superAdminCanEditCompletedAsNotHappenedMeeting() {
        meeting.setStatus(MeetingStatus.COMPLETED_AS_NOT_HAPPENED);
        setupSecurityContext("ROLE_SUPER_ADMIN");
        when(meetingRepository.findOne(any())).thenReturn(Optional.of(meeting));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);
        
        assertDoesNotThrow(() -> meetingService.updateMeeting(meetingId, teamCardId, updateDto));
        verify(meetingRepository, times(1)).save(any(Meeting.class));
    }
    
    @Test
    void adminCannotEditCompletedMeeting() {
        setupSecurityContext("ROLE_ADMIN");
        when(meetingRepository.findOne(any())).thenReturn(Optional.of(meeting));
        
        assertThrows(MeetingCompletedException.class, () -> meetingService.updateMeeting(meetingId, teamCardId, updateDto));
        verify(meetingRepository, never()).save(any(Meeting.class));
    }
    
    @Test
    void trackerCannotEditCompletedMeeting() {
        setupSecurityContext("ROLE_TRACKER");
        when(meetingRepository.findOne(any())).thenReturn(Optional.of(meeting));
        
        assertThrows(MeetingCompletedException.class, () -> meetingService.updateMeeting(meetingId, teamCardId, updateDto));
        verify(meetingRepository, never()).save(any(Meeting.class));
    }
    
    @Test
    void superAdminCanEditScheduledMeeting() {
        meeting.setStatus(MeetingStatus.SCHEDULED);
        setupSecurityContext("ROLE_SUPER_ADMIN");
        when(meetingRepository.findOne(any())).thenReturn(Optional.of(meeting));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);
        
        assertDoesNotThrow(() -> meetingService.updateMeeting(meetingId, teamCardId, updateDto));
        verify(meetingRepository, times(1)).save(any(Meeting.class));
    }
    
    @Test
    void adminCanEditScheduledMeeting() {
        meeting.setStatus(MeetingStatus.SCHEDULED);
        setupSecurityContext("ROLE_ADMIN");
        when(meetingRepository.findOne(any())).thenReturn(Optional.of(meeting));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);
        
        assertDoesNotThrow(() -> meetingService.updateMeeting(meetingId, teamCardId, updateDto));
        verify(meetingRepository, times(1)).save(any(Meeting.class));
    }
}
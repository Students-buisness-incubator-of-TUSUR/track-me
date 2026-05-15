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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingServiceImplSuperAdminTest {

    @Mock MeetingRepository meetingRepository;
    @Mock MeetingMapper meetingMapper;
    @Mock AclService aclService;
    @Mock MeetingEventsProducer meetingEventsProducer;
    @Mock BackendApiClient userBackendClient;
    @Mock SsoApiClient ssoApiClient;

    @InjectMocks MeetingServiceImpl meetingService;

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
                .recordLink("https://new-link.com")
                .tasksCurrentMeeting("new tasks")
                .build();
    }

    private void setAuth(String role) {
        Authentication auth = mock(Authentication.class);
        Collection<? extends GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        when(auth.getAuthorities()).thenReturn((Collection) authorities);
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
    }
    //g
    @SuppressWarnings("unchecked")
    @Test
    void superAdmin_canEdit_COMPLETED() {
        setAuth("ROLE_SUPER_ADMIN");
        when(meetingRepository.findOne(any(Specification.class))).thenReturn(Optional.of(meeting));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);
        
        assertDoesNotThrow(() -> meetingService.updateMeeting(meetingId, teamCardId, updateDto));
        verify(meetingRepository, times(1)).save(any(Meeting.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void superAdmin_canEdit_COMPLETED_AS_NOT_HAPPENED() {
        meeting.setStatus(MeetingStatus.COMPLETED_AS_NOT_HAPPENED);
        setAuth("ROLE_SUPER_ADMIN");
        when(meetingRepository.findOne(any(Specification.class))).thenReturn(Optional.of(meeting));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);
        
        assertDoesNotThrow(() -> meetingService.updateMeeting(meetingId, teamCardId, updateDto));
        verify(meetingRepository, times(1)).save(any(Meeting.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void admin_cannotEdit_COMPLETED() {
        setAuth("ROLE_ADMIN");
        when(meetingRepository.findOne(any(Specification.class))).thenReturn(Optional.of(meeting));
        
        assertThrows(MeetingCompletedException.class, 
            () -> meetingService.updateMeeting(meetingId, teamCardId, updateDto));
        verify(meetingRepository, never()).save(any(Meeting.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void tracker_cannotEdit_COMPLETED() {
        setAuth("ROLE_TRACKER");
        when(meetingRepository.findOne(any(Specification.class))).thenReturn(Optional.of(meeting));
        
        assertThrows(MeetingCompletedException.class, 
            () -> meetingService.updateMeeting(meetingId, teamCardId, updateDto));
        verify(meetingRepository, never()).save(any(Meeting.class));
    }
}
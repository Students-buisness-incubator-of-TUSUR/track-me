package net.trackme.meetingservice.services;

import net.trackme.commons.acl.AclService;
import net.trackme.meetingservice.api.dto.MeetingDto;
import net.trackme.meetingservice.api.dto.MeetingUpdateDto;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.entities.TeamStatus;
import net.trackme.meetingservice.mapping.MeetingMapper;
import net.trackme.meetingservice.messaging.own.MeetingEventsProducer;
import net.trackme.meetingservice.services.exceptions.MeetingCompletedException;
import net.trackme.meetingservice.services.exceptions.MeetingNotFoundException;
import net.trackme.meetingservice.services.integration.backend.BackendApiClient;
import net.trackme.meetingservice.services.integration.backend.dto.TeamCardDto;
import net.trackme.meetingservice.services.integration.sso.SsoApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MeetingServiceImplSuperAdminTest {

    @Mock private MeetingRepository meetingRepository;
    @Mock private MeetingMapper meetingMapper;
    @Mock private AclService aclService;
    @Mock private MeetingEventsProducer meetingEventsProducer;
    @Mock(name = "userBackendApiClient") private BackendApiClient userBackendClient;
    @Mock private SsoApiClient ssoApiClient;

    @InjectMocks
    private MeetingServiceImpl meetingService;

    private UUID meetingId;
    private UUID teamCardId;
    private Meeting meeting;
    private MeetingUpdateDto updateDto;

    private void setupAuthenticationWithRole(String role) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getAuthorities()).thenAnswer(invocation ->
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @BeforeEach
    void setUp() {
        meetingId = UUID.randomUUID();
        teamCardId = UUID.randomUUID();
        meeting = Meeting.builder()
                .id(meetingId)
                .teamCardId(teamCardId)
                .status(MeetingStatus.SCHEDULED)
                .startDate(OffsetDateTime.now())
                .build();
        updateDto = MeetingUpdateDto.builder()
                .recordLink("https://example.com/updated")
                .number("2")
                .teamStatus(TeamStatus.OK)
                .build();

        MeetingDto mockMeetingDto = mock(MeetingDto.class);
        lenient().when(meetingMapper.mapToDto(any())).thenReturn(mockMeetingDto);
        TeamCardDto teamCardDto = TeamCardDto.builder().meetingRoomLink("https://meeting.room").build();
        lenient().when(userBackendClient.getTeamCardById(any())).thenReturn(teamCardDto);
        lenient().when(meetingRepository.findOne(any(Specification.class))).thenReturn(Optional.of(meeting));
        lenient().when(meetingRepository.saveAndFlush(any(Meeting.class))).thenReturn(meeting);
        // Не мокаем findById в setUp – сделаем это в тестах, где нужно
        lenient().when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);
        lenient().doNothing().when(meetingMapper).updateEntityFromDto(any(), any());
    }

    // ========== ТЕСТЫ ДЛЯ updateMeeting ==========

    @Test
    void regularAdmin_canUpdateScheduledMeeting() {
        setupAuthenticationWithRole("ADMIN");
        meeting.setStatus(MeetingStatus.SCHEDULED);
        meetingService.updateMeeting(meetingId, teamCardId, updateDto);
        verify(meetingRepository).saveAndFlush(meeting);
    }

    @Test
    void regularAdmin_cannotUpdateCompletedMeeting() {
        setupAuthenticationWithRole("ADMIN");
        meeting.setStatus(MeetingStatus.COMPLETED);
        assertThatThrownBy(() -> meetingService.updateMeeting(meetingId, teamCardId, updateDto))
                .isInstanceOf(MeetingCompletedException.class);
        verify(meetingRepository, never()).saveAndFlush(any());
    }

    @Test
    void regularAdmin_cannotUpdateFinallyCompletedMeeting() {
        setupAuthenticationWithRole("ADMIN");
        meeting.setStatus(MeetingStatus.FINALLY_COMPLETED);
        assertThatThrownBy(() -> meetingService.updateMeeting(meetingId, teamCardId, updateDto))
                .isInstanceOf(MeetingCompletedException.class);
        verify(meetingRepository, never()).saveAndFlush(any());
    }

    @Test
    void regularAdmin_cannotUpdateCompletedAsNotHappenedMeeting() {
        setupAuthenticationWithRole("ADMIN");
        meeting.setStatus(MeetingStatus.COMPLETED_AS_NOT_HAPPENED);
        assertThatThrownBy(() -> meetingService.updateMeeting(meetingId, teamCardId, updateDto))
                .isInstanceOf(MeetingCompletedException.class);
        verify(meetingRepository, never()).saveAndFlush(any());
    }

    @Test
    void superAdmin_canUpdateFinallyCompletedMeeting() {
        setupAuthenticationWithRole("SUPER_ADMIN");
        meeting.setStatus(MeetingStatus.FINALLY_COMPLETED);
        meetingService.updateMeeting(meetingId, teamCardId, updateDto);
        verify(meetingRepository).saveAndFlush(meeting);
    }

    @Test
    void superAdmin_canUpdateCompletedAsNotHappenedMeeting() {
        setupAuthenticationWithRole("SUPER_ADMIN");
        meeting.setStatus(MeetingStatus.COMPLETED_AS_NOT_HAPPENED);
        meetingService.updateMeeting(meetingId, teamCardId, updateDto);
        verify(meetingRepository).saveAndFlush(meeting);
    }

    @Test
    void superAdmin_canUpdateOrdinaryCompletedMeeting() {
        setupAuthenticationWithRole("SUPER_ADMIN");
        meeting.setStatus(MeetingStatus.COMPLETED);
        meetingService.updateMeeting(meetingId, teamCardId, updateDto);
        verify(meetingRepository).saveAndFlush(meeting);
    }

    @Test
    void updateMeeting_throwsWhenMeetingNotFound() {
        setupAuthenticationWithRole("ADMIN");
        when(meetingRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> meetingService.updateMeeting(meetingId, teamCardId, updateDto))
                .isInstanceOf(MeetingNotFoundException.class);
    }

    // ========== ТЕСТЫ ДЛЯ updateBySuperAdmin ==========

    @Test
    void updateBySuperAdmin_superAdmin_canUpdateFinallyCompletedMeeting() {
        setupAuthenticationWithRole("SUPER_ADMIN");
        meeting.setStatus(MeetingStatus.FINALLY_COMPLETED);
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));
        meetingService.updateBySuperAdmin(meetingId, updateDto);
        verify(meetingRepository).save(meeting);
    }

    @Test
    void updateBySuperAdmin_superAdmin_canUpdateCompletedAsNotHappenedMeeting() {
        setupAuthenticationWithRole("SUPER_ADMIN");
        meeting.setStatus(MeetingStatus.COMPLETED_AS_NOT_HAPPENED);
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));
        meetingService.updateBySuperAdmin(meetingId, updateDto);
        verify(meetingRepository).save(meeting);
    }

    @Test
    void updateBySuperAdmin_superAdmin_throwsWhenStatusNotAllowed() {
        setupAuthenticationWithRole("SUPER_ADMIN");
        meeting.setStatus(MeetingStatus.COMPLETED);
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));
        assertThatThrownBy(() -> meetingService.updateBySuperAdmin(meetingId, updateDto))
                .isInstanceOf(IllegalStateException.class);
        verify(meetingRepository, never()).save(any());
    }

    @Test
    void updateBySuperAdmin_regularAdmin_throwsAccessDenied() {
        setupAuthenticationWithRole("ADMIN");
        meeting.setStatus(MeetingStatus.FINALLY_COMPLETED);
        // Не мокаем findById, так как исключение выбрасывается до его вызова
        assertThatThrownBy(() -> meetingService.updateBySuperAdmin(meetingId, updateDto))
                .isInstanceOf(AccessDeniedException.class);
        verify(meetingRepository, never()).save(any());
        verify(meetingRepository, never()).findById(any());
    }

    @Test
    void updateBySuperAdmin_throwsWhenMeetingNotFound() {
        setupAuthenticationWithRole("SUPER_ADMIN");
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> meetingService.updateBySuperAdmin(meetingId, updateDto))
                .isInstanceOf(MeetingNotFoundException.class);
    }

    // ========== ТЕСТЫ ДЛЯ MeetingStatus ==========

    @Test
    void meetingStatus_isEditableBySuperAdmin_ReturnsTrueForFinallyCompleted() {
        assertThat(MeetingStatus.FINALLY_COMPLETED.isEditableBySuperAdmin()).isTrue();
    }

    @Test
    void meetingStatus_isEditableBySuperAdmin_ReturnsTrueForCompletedAsNotHappened() {
        assertThat(MeetingStatus.COMPLETED_AS_NOT_HAPPENED.isEditableBySuperAdmin()).isTrue();
    }

    @Test
    void meetingStatus_isEditableBySuperAdmin_ReturnsFalseForScheduled() {
        assertThat(MeetingStatus.SCHEDULED.isEditableBySuperAdmin()).isFalse();
    }

    @Test
    void meetingStatus_isEditableBySuperAdmin_ReturnsFalseForCompleted() {
        assertThat(MeetingStatus.COMPLETED.isEditableBySuperAdmin()).isFalse();
    }

    @Test
    void meetingStatus_isEditableBySuperAdmin_ShouldBeTrueOnlyForTwoSpecificStatuses() {
        List<MeetingStatus> allStatuses = Arrays.asList(MeetingStatus.values());
        for (MeetingStatus status : allStatuses) {
            if (status == MeetingStatus.FINALLY_COMPLETED || status == MeetingStatus.COMPLETED_AS_NOT_HAPPENED) {
                assertThat(status.isEditableBySuperAdmin()).isTrue();
            } else {
                assertThat(status.isEditableBySuperAdmin()).isFalse();
            }
        }
    }

    @Test
    void meetingStatus_FINALLY_COMPLETED_ShouldBeEditable() {
        assertThat(MeetingStatus.FINALLY_COMPLETED.isEditableBySuperAdmin()).isTrue();
        assertThat(MeetingStatus.FINALLY_COMPLETED.getDescription()).isEqualTo("Окончательно завершена");
    }

    @Test
    void meetingStatus_COMPLETED_AS_NOT_HAPPENED_ShouldBeEditable() {
        assertThat(MeetingStatus.COMPLETED_AS_NOT_HAPPENED.isEditableBySuperAdmin()).isTrue();
        assertThat(MeetingStatus.COMPLETED_AS_NOT_HAPPENED.getDescription()).isEqualTo("Завершена как не состоявшаяся");
    }

    @Test
    void meetingStatus_OnlyTwoStatusesShouldBeEditable() {
        long editableCount = Arrays.stream(MeetingStatus.values())
                .filter(MeetingStatus::isEditableBySuperAdmin)
                .count();
        assertThat(editableCount).isEqualTo(2);
    }

    @Test
    void meetingStatus_EditableStatusesAreExactlyFinallyCompletedAndCompletedAsNotHappened() {
        List<MeetingStatus> editableStatuses = Arrays.stream(MeetingStatus.values())
                .filter(MeetingStatus::isEditableBySuperAdmin)
                .toList();
        assertThat(editableStatuses).containsExactlyInAnyOrder(
                MeetingStatus.FINALLY_COMPLETED,
                MeetingStatus.COMPLETED_AS_NOT_HAPPENED
        );
    }

    @Test
    void meetingStatus_GetDescription_ReturnsCorrectValue() {
        assertThat(MeetingStatus.FINALLY_COMPLETED.getDescription()).isEqualTo("Окончательно завершена");
        assertThat(MeetingStatus.COMPLETED_AS_NOT_HAPPENED.getDescription()).isEqualTo("Завершена как не состоявшаяся");
        assertThat(MeetingStatus.SCHEDULED.getDescription()).isEqualTo("Запланирована");
        assertThat(MeetingStatus.COMPLETED.getDescription()).isEqualTo("Завершена");
    }

    @Test
    void meetingStatus_COMPLETED_STATUSES_ContainsAllCompletedStatuses() {
        assertThat(MeetingStatus.COMPLETED_STATUSES).containsExactlyInAnyOrder(
                MeetingStatus.COMPLETED,
                MeetingStatus.FINALLY_COMPLETED,
                MeetingStatus.COMPLETED_AS_NOT_HAPPENED
        );
    }

    @Test
    void meetingStatus_toString_ReturnsEnumName() {
        //assertThat(MeetingStatus.FINALLY_COMPLETED.toString()).isEqualTo("FINALLY_COMPLETED");
        assertThat(MeetingStatus.FINALLY_COMPLETED.toString()).hasToString("FINALLY_COMPLETED");
        //assertThat(MeetingStatus.COMPLETED_AS_NOT_HAPPENED.toString()).isEqualTo("COMPLETED_AS_NOT_HAPPENED");
         assertThat(MeetingStatus.COMPLETED_AS_NOT_HAPPENED.toString()).hasToString("COMPLETED_AS_NOT_HAPPENED");
        //assertThat(MeetingStatus.SCHEDULED.toString()).isEqualTo("SCHEDULED");
        assertThat(MeetingStatus.SCHEDULED.toString()).hasToString("SCHEDULED");
        //assertThat(MeetingStatus.COMPLETED.toString()).isEqualTo("COMPLETED");
        assertThat(MeetingStatus.COMPLETED.toString()).hasToString("COMPLETED");
    }
}

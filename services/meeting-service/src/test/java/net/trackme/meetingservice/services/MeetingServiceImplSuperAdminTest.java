package net.trackme.meetingservice.services;

import net.trackme.meetingservice.api.MeetingUpdateDto;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MeetingServiceImplSuperAdminTest {

    @Test
    @WithMockUser(username = "superadmin", authorities = {"ROLE_SUPER_ADMIN"})
    void superAdminCanEditCompletedMeeting() {
        // given: встреча со статусом COMPLETED (Finally completed)
        UUID meetingId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID teamCardId = UUID.fromString("223e4567-e89b-12d3-a456-426614174001");
        
        MeetingUpdateDto updateDto = MeetingUpdateDto.builder()
                .startDate(OffsetDateTime.now().plusDays(1))
                .recordLink("https://new-record-link.com")
                .build();
        
        // when: суперадмин пытается редактировать
        // then: не должно быть исключения
        assertDoesNotThrow(() -> {
            meetingService.updateMeeting(meetingId, teamCardId, updateDto);
        });
    }

    @Test
    @WithMockUser(username = "superadmin", authorities = {"ROLE_SUPER_ADMIN"})
    void superAdminCanEditCompletedAsNotHappenedMeeting() {
        // given: встреча со статусом COMPLETED_AS_NOT_HAPPENED
        UUID meetingId = UUID.fromString("323e4567-e89b-12d3-a456-426614174002");
        UUID teamCardId = UUID.fromString("423e4567-e89b-12d3-a456-426614174003");
        
        MeetingUpdateDto updateDto = MeetingUpdateDto.builder()
                .startDate(OffsetDateTime.now().plusDays(2))
                .tasksCurrentMeeting("Новые задачи")
                .build();
        
        // when + then: не должно быть исключения
        assertDoesNotThrow(() -> {
            meetingService.updateMeeting(meetingId, teamCardId, updateDto);
        });
    }

    @Test
    @WithMockUser(username = "tracker", authorities = {"ROLE_TRACKER"})
    void trackerCannotEditCompletedMeeting() {
        // given: встреча со статусом COMPLETED
        // when: трекер пытается редактировать
        // then: должно быть исключение MeetingCompletedException
        assertThrows(MeetingCompletedException.class, () -> {
            meetingService.updateMeeting(meetingId, teamCardId, updateDto);
        });
    }
}

package net.trackme.meetingservice.services;

import net.trackme.meetingservice.api.dto.MeetingCreateDto;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.mapping.MeetingMapper;
import net.trackme.meetingservice.services.integration.backend.BackendApiClient;
import net.trackme.meetingservice.services.integration.backend.dto.StreamDto;
import net.trackme.meetingservice.services.integration.backend.dto.TeamCardDto;
import net.trackme.meetingservice.services.integration.sso.SsoApiClient;
import net.trackme.commons.acl.AclService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingServiceImplSimpleTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingMapper meetingMapper;

    @Mock
    private AclService aclService;

    @Mock
    private BackendApiClient userBackendApiClient;

    @Mock
    private SsoApiClient ssoApiClient;

    @InjectMocks
    private MeetingServiceImpl meetingService;

    private UUID activeTeamId;
    private TeamCardDto activeTeamCard;

    @BeforeEach
    void setUp() {
        activeTeamId = UUID.randomUUID();
        activeTeamCard = TeamCardDto.builder()
                .id(activeTeamId)
                .name("Active Team")
                .username("tracker")
                .passive(false)
                .streams(List.of(new StreamDto(UUID.randomUUID())))
                .meetingRoomLink("https://zoom.us/j/123")
                .build();
    }

    @Test
    @WithMockUser(username = "tracker", roles = "TRACKER")
    void createMeetingForActiveTeamShouldWork() {
        when(userBackendApiClient.getTeamCardById(activeTeamId)).thenReturn(activeTeamCard);

        MeetingCreateDto dto = MeetingCreateDto.builder()
                .number("1")
                .startDate(OffsetDateTime.now().plusDays(1))
                .build();

        Meeting mockMeeting = new Meeting();
        mockMeeting.setId(UUID.randomUUID());
        when(meetingMapper.mapToEntity(any(MeetingCreateDto.class))).thenReturn(mockMeeting);
        when(meetingRepository.save(any(Meeting.class))).thenReturn(mockMeeting);

        var result = meetingService.createMeeting(activeTeamId, dto);

        assertThat(result).isNotNull();
        assertThat(result.teamCardId()).isEqualTo(activeTeamId.toString());
    }
}
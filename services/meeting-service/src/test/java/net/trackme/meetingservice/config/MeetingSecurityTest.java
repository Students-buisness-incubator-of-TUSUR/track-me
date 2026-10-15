package net.trackme.meetingservice.configuration;

import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingSecurityTest {

    @Mock
    private MeetingRepository meetingRepository;

    @InjectMocks
    private MeetingSecurity meetingSecurity;

    private final UUID meetingId = UUID.randomUUID();

    // ============================================================
    // authentication == null
    // ============================================================

    @Test
    void isTrackerOfMeeting_authenticationIsNull_returnsFalse() {
        boolean result = meetingSecurity.isTrackerOfMeeting(meetingId, null);
        assertThat(result).isFalse();
    }

    // ============================================================
    // !authentication.isAuthenticated()
    // ============================================================

    @Test
    void isTrackerOfMeeting_authenticationNotAuthenticated_returnsFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        boolean result = meetingSecurity.isTrackerOfMeeting(meetingId, auth);
        assertThat(result).isFalse();
    }

    // ============================================================
    // username == null
    // ============================================================

    @Test
    void isTrackerOfMeeting_usernameIsNull_returnsFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn(null);

        boolean result = meetingSecurity.isTrackerOfMeeting(meetingId, auth);
        assertThat(result).isFalse();
    }

    // ============================================================
    // username.isBlank()
    // ============================================================

    @Test
    void isTrackerOfMeeting_usernameIsBlank_returnsFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("   ");

        boolean result = meetingSecurity.isTrackerOfMeeting(meetingId, auth);
        assertThat(result).isFalse();
    }

    // ============================================================
    // meeting not found
    // ============================================================

    @Test
    void isTrackerOfMeeting_meetingNotFound_returnsFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("tracker_user");
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.empty());

        boolean result = meetingSecurity.isTrackerOfMeeting(meetingId, auth);
        assertThat(result).isFalse();
    }

    // ============================================================
    // trackerUsername matches (equalsIgnoreCase)
    // ============================================================

    @Test
    void isTrackerOfMeeting_trackerUsernameMatches_returnsTrue() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("tracker_user");

        Meeting meeting = Meeting.builder()
                .id(meetingId)
                .trackerUsername("tracker_user")
                .build();
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));

        boolean result = meetingSecurity.isTrackerOfMeeting(meetingId, auth);
        assertThat(result).isTrue();
    }

    @Test
    void isTrackerOfMeeting_trackerUsernameMatchesDifferentCase_returnsTrue() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("Tracker_User");

        Meeting meeting = Meeting.builder()
                .id(meetingId)
                .trackerUsername("tracker_user")
                .build();
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));

        boolean result = meetingSecurity.isTrackerOfMeeting(meetingId, auth);
        assertThat(result).isTrue();
    }

    // ============================================================
    // trackerUsername doesn't match
    // ============================================================

    @Test
    void isTrackerOfMeeting_trackerUsernameDoesNotMatch_returnsFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("other_tracker");

        Meeting meeting = Meeting.builder()
                .id(meetingId)
                .trackerUsername("tracker_user")
                .build();
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));

        boolean result = meetingSecurity.isTrackerOfMeeting(meetingId, auth);
        assertThat(result).isFalse();
    }

    // ============================================================
    // trackerUsername == null
    // ============================================================

    @Test
    void isTrackerOfMeeting_meetingTrackerUsernameIsNull_returnsFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("tracker_user");

        Meeting meeting = Meeting.builder()
                .id(meetingId)
                .trackerUsername(null)
                .build();
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));

        boolean result = meetingSecurity.isTrackerOfMeeting(meetingId, auth);
        assertThat(result).isFalse();
    }

    // ============================================================
    // Real Authentication implementations (не mock)
    // ============================================================

    @Test
    void isTrackerOfMeeting_realUsernamePasswordAuth_returnsTrue() {
        var auth = new UsernamePasswordAuthenticationToken(
                "tracker_user", "n/a",
                AuthorityUtils.createAuthorityList("ROLE_TRACKER"));

        Meeting meeting = Meeting.builder()
                .id(meetingId)
                .trackerUsername("tracker_user")
                .build();
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));

        boolean result = meetingSecurity.isTrackerOfMeeting(meetingId, auth);
        assertThat(result).isTrue();
    }

    @Test
    void isTrackerOfMeeting_anonymousAuthentication_returnsFalse() {
        var auth = new AnonymousAuthenticationToken(
                "key", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

        boolean result = meetingSecurity.isTrackerOfMeeting(meetingId, auth);
        assertThat(result).isFalse();
    }
}

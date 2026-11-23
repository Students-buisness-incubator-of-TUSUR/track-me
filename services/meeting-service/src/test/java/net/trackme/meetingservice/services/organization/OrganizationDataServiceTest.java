package net.trackme.meetingservice.services.organization;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import net.trackme.commons.acl.AclService;
import net.trackme.meetingservice.dao.MeetingMetadataRepository;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import org.junit.jupiter.api.Test;

/**
 * Тесты каскадного удаления данных организации в meeting-сервисе.
 */
class OrganizationDataServiceTest {

  private final MeetingRepository meetings = mock(MeetingRepository.class);

  private final MeetingMetadataRepository metadata = mock(MeetingMetadataRepository.class);

  private final AclService aclService = mock(AclService.class);

  private final OrganizationDataService service =
      new OrganizationDataService(meetings, metadata, aclService);

  @Test
  void deletesMeetingsStreamsAndAclEntries() {
    var organizationId = UUID.randomUUID();
    var first = meeting();
    var second = meeting();
    when(meetings.findAllByOrganizationId(organizationId))
        .thenReturn(List.of(first, second));

    service.deleteOrganizationData(organizationId);

    verify(aclService).deleteAcl(first);
    verify(aclService).deleteAcl(second);
    verify(metadata).deleteMeetingStreamsByOrganizationId(organizationId);
    verify(metadata).deleteAllByOrganizationId(organizationId);
  }

  @Test
  void renameUpdatesMeetingOrganizationName() {
    var organizationId = UUID.randomUUID();

    service.renameOrganization(organizationId, "Новое название");

    verify(metadata).updateOrganizationName(organizationId, "Новое название");
  }

  @Test
  void missingAclDoesNotStopCascadeDeletion() {
    var organizationId = UUID.randomUUID();
    var meeting = meeting();
    when(meetings.findAllByOrganizationId(organizationId)).thenReturn(List.of(meeting));
    doThrow(new IllegalStateException("ACL not found"))
        .when(aclService).deleteAcl(any());

    service.deleteOrganizationData(organizationId);

    verify(metadata).deleteMeetingStreamsByOrganizationId(organizationId);
    verify(metadata).deleteAllByOrganizationId(organizationId);
  }

  private static Meeting meeting() {
    var meeting = new Meeting();
    meeting.setId(UUID.randomUUID());
    return meeting;
  }
}

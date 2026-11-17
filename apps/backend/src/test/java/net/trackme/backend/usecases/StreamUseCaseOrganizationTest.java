package net.trackme.backend.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.mapping.NtiMarketMapper;
import net.trackme.backend.mapping.StreamMapper;
import net.trackme.backend.services.organization.OrganizationContext;
import net.trackme.backend.services.stream.MutableStreamService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Тесты организационной области в публичном API потоков.
 */
class StreamUseCaseOrganizationTest {

  private static final UUID ORGANIZATION_ID = UUID.randomUUID();

  private static final UUID STREAM_ID = UUID.randomUUID();

  private final MutableStreamService streamService = mock(MutableStreamService.class);

  private final StreamMapper streamMapper = mock(StreamMapper.class);

  private final NtiMarketMapper ntiMarketMapper = mock(NtiMarketMapper.class);

  private final OrganizationContext organizationContext = mock(OrganizationContext.class);

  private final StreamUseCase useCase = new StreamUseCase(
      streamService, streamMapper, ntiMarketMapper, organizationContext);

  @Test
  void findAllActiveFiltersByOrganizationForRegularUser() {
    var pageable = PageRequest.of(0, 10);
    when(organizationContext.getOrganizationId()).thenReturn(Optional.of(ORGANIZATION_ID));
    when(organizationContext.isOverlord()).thenReturn(false);
    when(streamService.findAll(any(Specification.class), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of()));

    var result = useCase.findAllActive(pageable);

    assertThat(result).isEmpty();
    verify(streamService).findAll(any(Specification.class), eq(pageable));
  }

  @Test
  void findAllActiveDoesNotFilterForOverlord() {
    var pageable = PageRequest.of(0, 10);
    when(organizationContext.getOrganizationId()).thenReturn(Optional.of(ORGANIZATION_ID));
    when(organizationContext.isOverlord()).thenReturn(true);
    when(streamService.findAll(any(Specification.class), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of()));

    useCase.findAllActive(pageable);

    verify(streamService).findAll(any(Specification.class), eq(pageable));
  }

  @Test
  void getStreamsFiltersByOrganizationForRegularUser() {
    var pageable = PageRequest.of(0, 10);
    when(organizationContext.getOrganizationId()).thenReturn(Optional.of(ORGANIZATION_ID));
    when(organizationContext.isOverlord()).thenReturn(false);
    when(streamService.findAll(any(Specification.class), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of()));

    var result = useCase.getStreams(List.of(), pageable);

    assertThat(result).isEmpty();
    verify(streamService).findAll(any(Specification.class), eq(pageable));
  }

  @Test
  void getStreamImageChecksOrganizationAccess() {
    var stream = new Stream();
    stream.setOrganizationId(ORGANIZATION_ID);
    stream.setImageBytes(new byte[] {1, 2});
    when(streamService.getById(STREAM_ID)).thenReturn(stream);

    var image = useCase.getStreamImage(STREAM_ID);

    assertThat(image).containsExactly(1, 2);
    verify(organizationContext).checkOrganizationAccess(ORGANIZATION_ID);
  }

  @Test
  void addImageChecksOrganizationAccess() {
    var stream = new Stream();
    stream.setOrganizationId(ORGANIZATION_ID);
    var file = new MockMultipartFile("file", "image.png", "image/png", new byte[] {1});
    when(streamService.getById(STREAM_ID)).thenReturn(stream);

    useCase.addImage(STREAM_ID, file);

    verify(organizationContext).checkOrganizationAccess(ORGANIZATION_ID);
    verify(streamService).addImage(STREAM_ID, file);
  }
}

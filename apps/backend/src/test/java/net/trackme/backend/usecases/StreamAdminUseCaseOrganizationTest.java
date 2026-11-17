package net.trackme.backend.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.mapping.StreamMapper;
import net.trackme.backend.rest.api.stream.StreamCreateDto;
import net.trackme.backend.rest.api.stream.StreamUpdateDto;
import net.trackme.backend.services.nti.NtiMarketService;
import net.trackme.backend.services.organization.OrganizationContext;
import net.trackme.backend.services.stream.MutableStreamService;
import net.trackme.commons.filters.FilterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

/**
 * Тесты организационной области в административном API потоков.
 */
class StreamAdminUseCaseOrganizationTest {

  private static final UUID ORGANIZATION_ID = UUID.randomUUID();

  private static final String ORGANIZATION_NAME = "Организация А";

  private static final UUID STREAM_ID = UUID.randomUUID();

  private final StreamMapper streamMapper = mock(StreamMapper.class);

  private final MutableStreamService streamService = mock(MutableStreamService.class);

  private final NtiMarketService ntiMarketService = mock(NtiMarketService.class);

  private final OrganizationContext organizationContext = mock(OrganizationContext.class);

  private final StreamAdminUseCase useCase = new StreamAdminUseCase(
      streamMapper, streamService, ntiMarketService, organizationContext);

  @Test
  void createStreamAssignsCurrentOrganization() {
    var dto = createDto();
    var stream = new Stream();
    when(streamMapper.mapFromDto(dto)).thenReturn(stream);
    when(organizationContext.getOrganizationId()).thenReturn(Optional.of(ORGANIZATION_ID));
    when(organizationContext.getOrganizationName()).thenReturn(Optional.of(ORGANIZATION_NAME));
    when(ntiMarketService.getNtiMarkets(anyList())).thenReturn(List.of());
    when(streamService.create(stream)).thenReturn(stream);

    useCase.createStream(dto);

    assertThat(stream.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
    assertThat(stream.getOrganizationName()).isEqualTo(ORGANIZATION_NAME);
  }

  @Test
  void updateStreamChecksOrganizationAccess() {
    var stream = new Stream();
    stream.setOrganizationId(ORGANIZATION_ID);
    var updateDto = mock(StreamUpdateDto.class);
    when(updateDto.ntiMarketIds()).thenReturn(List.of());
    when(streamService.getById(STREAM_ID)).thenReturn(stream);
    when(ntiMarketService.getNtiMarkets(anyList())).thenReturn(List.of());
    when(streamService.save(stream)).thenReturn(stream);

    useCase.updateStream(STREAM_ID, updateDto);

    verify(organizationContext).checkOrganizationAccess(ORGANIZATION_ID);
  }

  @Test
  void deleteStreamChecksOrganizationAccess() {
    var stream = new Stream();
    stream.setOrganizationId(ORGANIZATION_ID);
    when(streamService.getById(STREAM_ID)).thenReturn(stream);

    useCase.deleteStream(STREAM_ID);

    verify(organizationContext).checkOrganizationAccess(ORGANIZATION_ID);
    verify(streamService).delete(STREAM_ID);
  }

  @Test
  void findAllStreamsFiltersByOrganizationForRegularUser() {
    var pageable = PageRequest.of(0, 10);
    var filterRequest = mock(FilterRequest.class);
    when(filterRequest.filters()).thenReturn(List.of());
    when(organizationContext.getOrganizationId()).thenReturn(Optional.of(ORGANIZATION_ID));
    when(organizationContext.isOverlord()).thenReturn(false);
    when(streamService.findAll(any(Specification.class), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of()));

    var result = useCase.findAllStreams(filterRequest, pageable);

    assertThat(result).isEmpty();
    verify(streamService).findAll(any(Specification.class), eq(pageable));
  }

  @Test
  void getByIdChecksOrganizationAccess() {
    var stream = new Stream();
    stream.setOrganizationId(ORGANIZATION_ID);
    when(streamService.getById(STREAM_ID)).thenReturn(stream);

    useCase.getById(STREAM_ID);

    verify(organizationContext).checkOrganizationAccess(ORGANIZATION_ID);
  }

  private static StreamCreateDto createDto() {
    return new StreamCreateDto("Поток", null, null, List.of(), null, null, null);
  }
}

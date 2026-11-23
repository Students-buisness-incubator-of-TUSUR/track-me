package net.trackme.backend.usecases;

import lombok.RequiredArgsConstructor;
import net.trackme.backend.domain.spec.StreamSpecification;
import net.trackme.backend.mapping.NtiMarketMapper;
import net.trackme.backend.mapping.StreamMapper;
import net.trackme.backend.rest.api.dto.NTIMarketDto;
import net.trackme.backend.rest.api.stream.StreamDto;
import net.trackme.backend.services.organization.OrganizationContext;
import net.trackme.backend.services.stream.MutableStreamService;
import net.trackme.commons.filters.Filter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StreamUseCase {

  private final MutableStreamService streamService;

  private final StreamMapper streamMapper;

  private final NtiMarketMapper ntiMarketMapper;

  private final OrganizationContext organizationContext;


  public Page<StreamDto> findAllActive(Pageable pageable) {
    var specification = StreamSpecification.currentlyActive();
    var organizationId = organizationContext.getOrganizationId();
    if (organizationId.isPresent() && !organizationContext.isOverlord()) {
      specification = specification.and(
          StreamSpecification.organizationEquals(organizationId.get()));
    }
    return streamService.findAll(specification, pageable)
        .map(streamMapper::mapToDto);
  }

  public Page<StreamDto> getStreams(List<Filter> filters, Pageable pageable) {
    var specification = StreamSpecification.withFilters(filters);
    var organizationId = organizationContext.getOrganizationId();
    if (organizationId.isPresent() && !organizationContext.isOverlord()) {
      specification = specification.and(
          StreamSpecification.organizationEquals(organizationId.get()));
    }
    var streams = streamService.findAll(specification, pageable);
    return streams.map(streamMapper::mapToDto);
  }

  public List<NTIMarketDto> getNTIMarkets() {
    var ntiMarkets = streamService.getNTIMarkets();
    return ntiMarkets.stream()
        .map(ntiMarketMapper::mapToDto)
        .toList();
  }


  public byte[] getStreamImage(UUID streamId) {
    var stream = streamService.getById(streamId);
    organizationContext.checkOrganizationAccess(stream.getOrganizationId());
    return stream.getImageBytes();
  }

  public void addImage(UUID streamId, MultipartFile file) {
    var stream = streamService.getById(streamId);
    organizationContext.checkOrganizationAccess(stream.getOrganizationId());
    streamService.addImage(streamId, file);
  }
}

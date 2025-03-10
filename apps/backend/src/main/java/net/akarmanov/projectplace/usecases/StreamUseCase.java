package net.akarmanov.projectplace.usecases;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.domain.spec.StreamSpecification;
import net.akarmanov.projectplace.filters.Filter;
import net.akarmanov.projectplace.mapping.NtiMarketMapper;
import net.akarmanov.projectplace.mapping.StreamMapper;
import net.akarmanov.projectplace.rest.api.dto.NTIMarketDto;
import net.akarmanov.projectplace.rest.api.dto.StreamDto;
import net.akarmanov.projectplace.services.stream.StreamService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StreamUseCase {

  private final StreamService streamService;

  private final StreamMapper streamMapper;

  private final NtiMarketMapper ntiMarketMapper;


  public Page<StreamDto> findAllActive(Pageable pageable) {
    return streamService.findAllActive(pageable)
        .map(streamMapper::mapToDto);
  }

  public Page<StreamDto> getStreams(List<Filter> filters, Pageable pageable) {
    var specification = StreamSpecification.withFilters(filters);
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
    return stream.getImageBytes();
  }

  public void addImage(UUID streamId, MultipartFile file) {
    streamService.addImage(streamId, file);
  }
}

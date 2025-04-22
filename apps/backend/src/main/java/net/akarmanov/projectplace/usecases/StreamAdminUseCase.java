package net.akarmanov.projectplace.usecases;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.commons.filters.FilterRequest;
import net.akarmanov.projectplace.domain.spec.StreamSpecification;
import net.akarmanov.projectplace.mapping.StreamMapper;
import net.akarmanov.projectplace.rest.api.dto.StreamCreateDto;
import net.akarmanov.projectplace.rest.api.dto.StreamDto;
import net.akarmanov.projectplace.rest.api.dto.StreamUpdateDto;
import net.akarmanov.projectplace.services.nti.NtiMarketService;
import net.akarmanov.projectplace.services.stream.StreamService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StreamAdminUseCase {

  private final StreamMapper streamMapper;

  private final StreamService streamService;

  private final NtiMarketService ntiMarketService;

  @Transactional
  public StreamDto createStream(StreamCreateDto streamCreateDto) {
    var ntiMarketIds = streamCreateDto.ntiMarketIds();
    var stream = streamMapper.mapFromDto(streamCreateDto);
    stream.addNtiMarkets(ntiMarketService.getNtiMarkets(ntiMarketIds));
    stream = streamService.create(stream);
    return streamMapper.mapToDto(stream);
  }

  @Transactional
  public StreamDto updateStream(UUID streamId, StreamUpdateDto stream) {
    var ntiMarketIds = stream.ntiMarketIds();
    var oldStream = streamService.getById(streamId);
    streamMapper.updateFromDto(stream, oldStream);
    oldStream.updateNtiMarkets(ntiMarketService.getNtiMarkets(ntiMarketIds));
    oldStream = streamService.save(oldStream);
    return streamMapper.mapToDto(oldStream);
  }

  public void deleteStream(UUID streamId) {
    streamService.delete(streamId);
  }

  public Page<StreamDto> findAllStreams(FilterRequest filterRequest, Pageable pageable) {
    var filters = filterRequest.filters();
    var specification = StreamSpecification.withFilters(filters);
    return streamService.findAll(specification, pageable)
        .map(streamMapper::mapToDto);
  }

  public StreamDto getById(UUID streamId) {
    var stream = streamService.getById(streamId);
    return streamMapper.mapToDto(stream);
  }

}

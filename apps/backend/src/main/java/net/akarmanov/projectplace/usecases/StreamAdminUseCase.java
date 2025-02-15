package net.akarmanov.projectplace.usecases;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.domain.spec.StreamSpecification;
import net.akarmanov.projectplace.filters.FilterRequest;
import net.akarmanov.projectplace.mapping.StreamMapper;
import net.akarmanov.projectplace.rest.api.dto.StreamCreateDto;
import net.akarmanov.projectplace.rest.api.dto.StreamDto;
import net.akarmanov.projectplace.rest.api.dto.StreamUpdateDto;
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

  public StreamDto createStream(StreamCreateDto streamCreateDto) {
    var stream = streamMapper.mapFromDto(streamCreateDto);
    stream = streamService.create(stream);
    return streamMapper.mapToDto(stream);
  }

  public StreamDto updateStream(UUID streamId, StreamUpdateDto stream) {
    var oldStream = streamService.getById(streamId);
    streamMapper.updateFromDto(stream, oldStream);
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

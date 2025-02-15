package net.akarmanov.projectplace.rest.api.admin;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.filters.FilterRequest;
import net.akarmanov.projectplace.rest.api.dto.StreamCreateDto;
import net.akarmanov.projectplace.rest.api.dto.StreamDto;
import net.akarmanov.projectplace.rest.api.dto.StreamUpdateDto;
import net.akarmanov.projectplace.usecases.StreamAdminUseCase;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class StreamAdminRestControllerImpl implements StreamAdminRestController {

  private final StreamAdminUseCase streamAdminUseCase;

  @Override
  public ResponseEntity<StreamDto> create(StreamCreateDto stream) {
    var streamDto = streamAdminUseCase.createStream(stream);
    return ResponseEntity.ok(streamDto);
  }

  @Override
  public ResponseEntity<StreamDto> update(UUID streamId, StreamUpdateDto stream) {
    var streamDto = streamAdminUseCase.updateStream(streamId, stream);
    return ResponseEntity.ok(streamDto);
  }

  @Override
  public ResponseEntity<Void> delete(UUID streamId) {
    streamAdminUseCase.deleteStream(streamId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public PagedModel<StreamDto> findAll(FilterRequest filterRequest, Pageable pageable) {
    var streams = streamAdminUseCase.findAllStreams(filterRequest, pageable);
    return new PagedModel<>(streams);
  }

  @Override
  public ResponseEntity<StreamDto> getById(UUID streamId) {
    var stream = streamAdminUseCase.getById(streamId);
    return ResponseEntity.ok(stream);
  }
}

package net.akarmanov.projectplace.rest.api.stream;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.filters.FilterRequest;
import net.akarmanov.projectplace.rest.api.dto.NTIMarketDto;
import net.akarmanov.projectplace.rest.api.dto.StreamDto;
import net.akarmanov.projectplace.usecases.StreamUseCase;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class StreamRestControllerImpl implements StreamRestController {

  private final StreamUseCase streamUseCase;

  @Override
  public ResponseEntity<StreamDto> getCurrentStream() {
    var stream = streamUseCase.getCurrentStream();
    return ResponseEntity.ok(stream);
  }

  @Override
  public PagedModel<StreamDto> getStreams(FilterRequest filterRequest, Pageable pageable) {
    var streams = streamUseCase.getStreams(filterRequest.filters(), pageable);
    return new PagedModel<>(streams);
  }

  @Override
  public ResponseEntity<Resource> getStreamImage(UUID streamId) {
    var image = streamUseCase.getStreamImage(streamId);
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .body(new ByteArrayResource(image));
  }

  @Override
  public ResponseEntity<Void> addImage(UUID streamId, MultipartFile file) {
    if (file.isEmpty()) {
      throw new StreamEmptyImageException();
    }

    if (file.getSize() > MAX_FILE_SIZE) {
      throw new StreamLargeImageSizeException();
    }

    streamUseCase.addImage(streamId, file);
    return ResponseEntity.ok().build();
  }

  @Override
  public List<NTIMarketDto> getNTIMarkets() {
    return streamUseCase.getNTIMarkets();
  }
}

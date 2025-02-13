package net.akarmanov.projectplace.rest.api.stream;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.rest.api.dto.NTIMarketDto;
import net.akarmanov.projectplace.rest.api.dto.StreamDto;
import net.akarmanov.projectplace.usecases.StreamUseCase;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
  public PagedModel<StreamDto> getStreams(Pageable pageable) {
    var streams = streamUseCase.getStreams(pageable);
    return new PagedModel<>(streams);
  }

  @Override
  public List<NTIMarketDto> getNTIMarkets() {
    return streamUseCase.getNTIMarkets();
  }
}

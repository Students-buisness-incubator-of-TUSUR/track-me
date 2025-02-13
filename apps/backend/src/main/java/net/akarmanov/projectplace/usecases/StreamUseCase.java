package net.akarmanov.projectplace.usecases;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.mapping.NtiMarketMapper;
import net.akarmanov.projectplace.mapping.StreamMapper;
import net.akarmanov.projectplace.rest.api.dto.NTIMarketDto;
import net.akarmanov.projectplace.rest.api.dto.StreamDto;
import net.akarmanov.projectplace.services.stream.StreamService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StreamUseCase {

  private final StreamService streamService;

  private final StreamMapper streamMapper;

  private final NtiMarketMapper ntiMarketMapper;


  public StreamDto getCurrentStream() {
    var stream = streamService.getCurrentStream();
    return streamMapper.mapToDto(stream);
  }

  public Page<StreamDto> getStreams(Pageable pageable) {
    var streams = streamService.findAll(pageable);
    return streams.map(streamMapper::mapToDto);
  }

  public List<NTIMarketDto> getNTIMarkets() {
    var ntiMarkets = streamService.getNTIMarkets();
    return ntiMarkets.stream()
        .map(ntiMarketMapper::mapToDto)
        .toList();
  }
}

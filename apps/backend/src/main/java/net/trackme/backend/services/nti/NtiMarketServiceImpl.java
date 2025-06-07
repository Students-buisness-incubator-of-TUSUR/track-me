package net.trackme.backend.services.nti;

import lombok.RequiredArgsConstructor;
import net.trackme.backend.domain.NTIMarket;
import net.trackme.backend.repos.NtiMarketRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NtiMarketServiceImpl implements NtiMarketService {

  private final NtiMarketRepository ntiMarketRepository;

  @Override
  public NTIMarket getNtiMarket(UUID ntiMarketId) {
    return ntiMarketRepository.findById(ntiMarketId)
        .orElseThrow(() -> new NtiMarketNotFoundException(ntiMarketId));
  }

  @Override
  public List<NTIMarket> getNtiMarkets(List<UUID> ids) {
    return ntiMarketRepository.findAllById(ids);
  }
}

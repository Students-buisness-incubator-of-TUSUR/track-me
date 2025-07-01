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
  public List<NTIMarket> getNtiMarkets(List<UUID> ids) {
    var ntiMarkets = ntiMarketRepository.findAllById(ids);
    if (ntiMarkets.isEmpty()) {
      throw new NtiMarketNotFoundException(ids);
    }
    return ntiMarkets;
  }
}

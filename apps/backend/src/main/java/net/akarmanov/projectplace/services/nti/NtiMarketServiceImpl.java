package net.akarmanov.projectplace.services.nti;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.domain.NTIMarket;
import net.akarmanov.projectplace.repos.NtiMarketRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NtiMarketServiceImpl implements NtiMarketService {

  private final NtiMarketRepository ntiMarketRepository;

  @Override
  public void checkNtiMarket(UUID ntiMarketId) {
    if (!ntiMarketRepository.existsById(ntiMarketId)) {
      throw new NtiMarketNotFoundException(ntiMarketId);
    }
  }

  @Override
  public NTIMarket getNtiMarket(UUID ntiMarketId) {
    return ntiMarketRepository.findById(ntiMarketId)
        .orElseThrow(() -> new NtiMarketNotFoundException(ntiMarketId));
  }

  @Override
  public List<NTIMarket> getNtiMarkets() {
    return ntiMarketRepository.findAll(Sort.sort(NTIMarket.class)
        .by(NTIMarket::getName)
        .ascending());
  }
}

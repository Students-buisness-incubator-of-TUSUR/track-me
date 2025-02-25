package net.akarmanov.projectplace.services.nti;

import net.akarmanov.projectplace.domain.NTIMarket;

import java.util.List;
import java.util.UUID;

public interface NtiMarketService {
  void checkNtiMarket(UUID ntiMarketId);

  NTIMarket getNtiMarket(UUID ntiMarketId);

  List<NTIMarket> getNtiMarkets();

  List<NTIMarket> getNtiMarkets(List<UUID> ids);
}

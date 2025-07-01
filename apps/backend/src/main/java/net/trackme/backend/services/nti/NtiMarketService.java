package net.trackme.backend.services.nti;

import net.trackme.backend.domain.NTIMarket;

import java.util.List;
import java.util.UUID;

public interface NtiMarketService {

  List<NTIMarket> getNtiMarkets(List<UUID> ids);
}

package net.akarmanov.projectplace.repos;

import net.akarmanov.projectplace.domain.NTIMarket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NtiMarketRepository extends JpaRepository<NTIMarket, UUID> {
  NTIMarket findByName(String name);
}

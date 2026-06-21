package net.trackme.backend.repos;

import net.trackme.backend.domain.NTIMarket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NtiMarketRepository extends JpaRepository<NTIMarket, UUID> {
}

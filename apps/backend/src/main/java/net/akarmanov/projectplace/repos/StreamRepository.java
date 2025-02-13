package net.akarmanov.projectplace.repos;

import net.akarmanov.projectplace.domain.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface StreamRepository extends JpaRepository<Stream, UUID>, JpaSpecificationExecutor<Stream> {
  Optional<Stream> getFirstByEndDateAfter(LocalDate now);
}

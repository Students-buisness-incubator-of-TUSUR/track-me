package net.akarmanov.projectplace.repos;

import net.akarmanov.projectplace.domain.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface StreamRepository extends JpaRepository<Stream, UUID>, JpaSpecificationExecutor<Stream> {

}

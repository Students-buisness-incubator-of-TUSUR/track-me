package net.trackme.backend.repos;

import net.trackme.backend.domain.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface StreamRepository extends JpaRepository<Stream, UUID>, JpaSpecificationExecutor<Stream> {

}

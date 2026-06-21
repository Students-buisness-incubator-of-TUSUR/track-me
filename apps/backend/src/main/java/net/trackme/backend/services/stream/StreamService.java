package net.trackme.backend.services.stream;

import net.trackme.backend.domain.NTIMarket;
import net.trackme.backend.domain.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

public interface StreamService {

  Stream getById(UUID id);

  Page<Stream> findAllActive(Pageable pageable);

  List<Stream> findAllActive();

  Page<Stream> findAll(Specification<Stream> specification, Pageable pageable);

  List<Stream> findAll();

  List<NTIMarket> getNTIMarkets();

  Stream findActive(UUID id);
}

package net.akarmanov.projectplace.services.stream;

import net.akarmanov.projectplace.domain.NTIMarket;
import net.akarmanov.projectplace.domain.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

public interface StreamService {
  Stream create(Stream createdStream);

  Stream getById(UUID id);

  Stream getCurrentStream();

  Stream save(Stream stream);

  Page<Stream> find(Pageable pageable);

  void delete(UUID streamId);

  Page<Stream> findAll(Specification<Stream> specification, Pageable pageable);

  List<NTIMarket> getNTIMarkets();
}

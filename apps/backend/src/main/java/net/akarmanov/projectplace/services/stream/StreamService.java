package net.akarmanov.projectplace.services.stream;

import net.akarmanov.projectplace.domain.NTIMarket;
import net.akarmanov.projectplace.domain.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface StreamService {
  Stream create(Stream createdStream);

  Stream getById(UUID id);

  Page<Stream> findAllActive(Pageable pageable);

  Stream save(Stream stream);

  void delete(UUID streamId);

  Page<Stream> findAll(Specification<Stream> specification, Pageable pageable);

  List<NTIMarket> getNTIMarkets();

  void addImage(UUID streamId, MultipartFile file);

  Stream findActive(UUID id);
}

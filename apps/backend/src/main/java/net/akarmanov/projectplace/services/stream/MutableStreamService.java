package net.akarmanov.projectplace.services.stream;

import net.akarmanov.projectplace.domain.Stream;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface MutableStreamService extends StreamService {
  Stream create(Stream createdStream);

  void delete(UUID streamId);

  Stream save(Stream stream);

  void addImage(UUID streamId, MultipartFile file);
}

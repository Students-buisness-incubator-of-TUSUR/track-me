package net.trackme.backend.services.stream;

import jakarta.transaction.Transactional;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.repos.NtiMarketRepository;
import net.trackme.backend.repos.StreamRepository;
import net.trackme.backend.services.exceptions.ImageUploadException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@Transactional
@PreAuthorize("hasRole('ADMIN')")
public class MutableDefaultStreamService extends AbstractStreamService implements MutableStreamService {

  public MutableDefaultStreamService(StreamRepository streamRepository,
                                     NtiMarketRepository ntiMarketRepository) {
    super(streamRepository, ntiMarketRepository);
  }


  @Override
  public Stream create(Stream createdStream) {
    return streamRepository.save(createdStream);
  }

  @Override
  public Stream save(Stream stream) {
    return streamRepository.save(stream);
  }

  @Override
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public void delete(UUID streamId) {
    streamRepository.deleteById(streamId);
  }

  @Override
  public void addImage(UUID streamId, MultipartFile file) {
    var stream = getById(streamId);
    try {
      stream.setImageBytes(file.getBytes());
      streamRepository.save(stream);
    } catch (Exception e) {
      throw new ImageUploadException(e);
    }
  }
}

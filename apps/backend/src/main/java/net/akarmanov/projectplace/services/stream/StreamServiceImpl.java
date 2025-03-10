package net.akarmanov.projectplace.services.stream;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.domain.NTIMarket;
import net.akarmanov.projectplace.domain.Stream;
import net.akarmanov.projectplace.repos.NtiMarketRepository;
import net.akarmanov.projectplace.repos.StreamRepository;
import net.akarmanov.projectplace.services.exceptions.StreamImageUploadException;
import net.akarmanov.projectplace.services.exceptions.StreamNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static net.akarmanov.projectplace.domain.spec.StreamSpecification.byActive;
import static net.akarmanov.projectplace.domain.spec.StreamSpecification.byId;
import static org.springframework.data.jpa.domain.Specification.where;

@Service
@Transactional
@RequiredArgsConstructor
public class StreamServiceImpl implements StreamService {

  private final StreamRepository streamRepository;

  private final NtiMarketRepository ntiMarketRepository;

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public Stream create(Stream createdStream) {
    return streamRepository.save(createdStream);
  }

  @Override
  public Stream getById(UUID id) {
    return streamRepository.findById(id)
        .orElseThrow(() -> new StreamNotFoundException(id));
  }

  @Override
  public Page<Stream> findAllActive(Pageable pageable) {
    return streamRepository.findAllByActiveTrue(pageable);
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
  public Page<Stream> findAll(Specification<Stream> specification, Pageable pageable) {
    return streamRepository.findAll(specification, pageable);
  }

  @Override
  public List<NTIMarket> getNTIMarkets() {
    return ntiMarketRepository.findAll();
  }

  @Override
  public void addImage(UUID streamId, MultipartFile file) {
    var stream = getById(streamId);
    try {
      stream.setImageBytes(file.getBytes());
      streamRepository.save(stream);
    } catch (Exception e) {
      throw new StreamImageUploadException(e);
    }
  }

  @Override
  public Stream findActive(UUID id) {
    return streamRepository.findOne(where(byActive())
            .and(byId(id)))
        .orElseThrow(() -> new ActiveStreamNotFoundException(id));
  }
}

package net.akarmanov.projectplace.services.stream;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.domain.NTIMarket;
import net.akarmanov.projectplace.domain.Stream;
import net.akarmanov.projectplace.repos.NtiMarketRepository;
import net.akarmanov.projectplace.repos.StreamRepository;
import net.akarmanov.projectplace.services.exceptions.CurrentStreamNotExistsException;
import net.akarmanov.projectplace.services.exceptions.StreamNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
    public Stream getCurrentStream() {
        return streamRepository.getFirstByEndDateAfter(LocalDate.now())
                .orElseThrow(CurrentStreamNotExistsException::new);
    }

    @Override
    public Stream save(Stream stream) {
        return streamRepository.save(stream);
    }

    @Override
    public Page<Stream> find(Pageable pageable) {
        return streamRepository.findAll(pageable);
    }

    @Override
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void delete(UUID streamId) {
        streamRepository.deleteById(streamId);
    }

    @Override
    public Page<Stream> findAll(Pageable pageable) {
        return streamRepository.findAll(pageable);
    }

    @Override
    public List<NTIMarket> getNTIMarkets() {
        return ntiMarketRepository.findAll();
    }
}

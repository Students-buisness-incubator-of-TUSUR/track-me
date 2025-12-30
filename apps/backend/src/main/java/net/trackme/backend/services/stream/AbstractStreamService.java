package net.trackme.backend.services.stream;

import lombok.RequiredArgsConstructor;
import net.trackme.backend.domain.NTIMarket;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.repos.NtiMarketRepository;
import net.trackme.backend.repos.StreamRepository;
import net.trackme.backend.services.exceptions.StreamNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static net.trackme.backend.domain.spec.StreamSpecification.byId;
import static net.trackme.backend.domain.spec.StreamSpecification.currentlyActive;


@RequiredArgsConstructor
public abstract class AbstractStreamService implements StreamService {

    protected final StreamRepository streamRepository;

    protected final NtiMarketRepository ntiMarketRepository;

    @Override
    public Stream getById(UUID id) {
        return streamRepository.findById(id)
                .orElseThrow(() -> new StreamNotFoundException(id));
    }

    @Override
    public Page<Stream> findAllActive(Pageable pageable) {
        return streamRepository.findAll(currentlyActive(), pageable);
    }

    @Override
    public List<Stream> findAllActive() {
        return streamRepository.findAll(currentlyActive());
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
    public Stream findActive(UUID id) {
        return streamRepository.findOne(currentlyActive()
                        .and(byId(id)))
                .orElseThrow(() -> new ActiveStreamNotFoundException(id));
    }
}

package net.akarmanov.projectplace.services.stream;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.domain.NTIMarket;
import net.akarmanov.projectplace.domain.Stream;
import net.akarmanov.projectplace.repos.NtiMarketRepository;
import net.akarmanov.projectplace.repos.StreamRepository;
import net.akarmanov.projectplace.services.exceptions.StreamNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static net.akarmanov.projectplace.domain.spec.StreamSpecification.byId;
import static net.akarmanov.projectplace.domain.spec.StreamSpecification.currentlyActive;
import static org.springframework.data.jpa.domain.Specification.where;


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
    public Page<Stream> findAll(Specification<Stream> specification, Pageable pageable) {
        return streamRepository.findAll(specification, pageable);
    }

    @Override
    public List<NTIMarket> getNTIMarkets() {
        return ntiMarketRepository.findAll();
    }

    @Override
    public Stream findActive(UUID id) {
        return streamRepository.findOne(where(currentlyActive())
                        .and(byId(id)))
                .orElseThrow(() -> new ActiveStreamNotFoundException(id));
    }
}

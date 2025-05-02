package net.akarmanov.projectplace.usecases;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.akarmanov.projectplace.commons.filters.FilterRequest;
import net.akarmanov.projectplace.domain.Stream;
import net.akarmanov.projectplace.domain.spec.StreamSpecification;
import net.akarmanov.projectplace.mapping.StreamMapper;
import net.akarmanov.projectplace.rest.api.dto.StreamCreateDto;
import net.akarmanov.projectplace.rest.api.dto.StreamDto;
import net.akarmanov.projectplace.rest.api.dto.StreamUpdateDto;
import net.akarmanov.projectplace.services.nti.NtiMarketService;
import net.akarmanov.projectplace.services.stream.MutableStreamService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreamAdminUseCase {

    private static final String STREAM_NOT_FOUND_MESSAGE = "Stream not found: ";

    private final StreamMapper streamMapper;
    private final MutableStreamService streamService;
    private final NtiMarketService ntiMarketService;

    private static void validateStreamEntityFound(UUID streamId, Stream streamEntity) {
        if (streamEntity == null) {
            String errorMessage = STREAM_NOT_FOUND_MESSAGE + streamId;
            log.warn(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Создать новый поток.
     */
    @Transactional
    public StreamDto createStream(StreamCreateDto streamCreateDto) {
        var streamEntity = streamMapper.mapFromDto(streamCreateDto);
        var ntiMarkets = ntiMarketService.getNtiMarkets(streamCreateDto.ntiMarketIds());
        streamEntity.addNtiMarkets(ntiMarkets);

        var savedStream = streamService.create(streamEntity);

        log.info("Stream created: {}", savedStream.getId());
        return streamMapper.mapToDto(savedStream);
    }

    /**
     * Обновить существующий поток.
     */
    @Transactional
    public StreamDto updateStream(UUID streamId, StreamUpdateDto updateDto) {
        var streamEntity = streamService.getById(streamId);
        validateStreamEntityFound(streamId, streamEntity);

        streamMapper.updateFromDto(updateDto, streamEntity);

        var ntiMarkets = ntiMarketService.getNtiMarkets(updateDto.ntiMarketIds());
        streamEntity.updateNtiMarkets(ntiMarkets);

        var savedStream = streamService.save(streamEntity);

        log.info("Stream updated: {}", streamId);
        return streamMapper.mapToDto(savedStream);
    }

    /**
     * Удалить поток.
     */
    public void deleteStream(UUID streamId) {
        streamService.delete(streamId);
        log.info("Stream deleted: {}", streamId);
    }

    /**
     * Получить потоки по фильтру с пагинацией.
     */
    public Page<StreamDto> findAllStreams(FilterRequest filterRequest, Pageable pageable) {
        var filters = filterRequest.filters();
        var specification = StreamSpecification.withFilters(filters);
        var page = streamService.findAll(specification, pageable);
        return page.map(streamMapper::mapToDto);
    }

    /**
     * Получить поток по идентификатору.
     */
    public StreamDto getById(UUID streamId) {
        var streamEntity = streamService.getById(streamId);
        validateStreamEntityFound(streamId, streamEntity);
        return streamMapper.mapToDto(streamEntity);
    }
}
package net.trackme.meetingservice.services.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.commons.filters.Filter;
import net.trackme.meetingservice.api.dto.MeetingReportRecordDto;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.mapping.MeetingMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static net.trackme.meetingservice.entities.MeetingSpecification.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingsReportServiceImpl implements MeetingsReportService {
    private static final String DEFAULT_GROUP_BY_SORT_FIELD = "teamName";
    private static final Sort.Direction DEFAULT_GROUP_BY_SORT_DIRECTION = Sort.Direction.ASC;
    private static final Sort  DEFAULT_GROUP_BY_SORT = Sort.by(
        DEFAULT_GROUP_BY_SORT_DIRECTION,
        DEFAULT_GROUP_BY_SORT_FIELD
    );

    private static final String DEFAULT_INNER_SORT_FIELD = "startDate";
    private static final Sort.Direction DEFAULT_INNER_SORT_DIRECTION = Sort.Direction.DESC;
    private static final Sort DEFAULT_INNER_SORT = Sort.by(
        DEFAULT_INNER_SORT_DIRECTION,
        DEFAULT_INNER_SORT_FIELD
    );

    private static final Sort DEFAULT_SORT = DEFAULT_GROUP_BY_SORT.and(DEFAULT_INNER_SORT);

    private final MeetingRepository meetingRepository;
    private final MeetingMapper meetingMapper;
    private final MeetingsReportExcelGenerator excelGenerator;

    @Override
    @Cacheable(value = "meetings-report-all", key = "#streamId.toString() + '-' + #filters.hashCode()")
    public List<MeetingReportRecordDto> getReportRecordsForStream(
            UUID streamId,
            List<Filter> filters
    ) {
        Specification<Meeting> spec = buildBaseSpec(streamId, filters);
        List<Meeting> meetings = meetingRepository.findAll(spec, DEFAULT_SORT);
        return meetings.stream()
                .map(meetingMapper::mapToReportDto)
                .toList();
    }

    @Override
    @Cacheable(value = "meetings-report-page",
               key = "#streamId.toString() + '-' + #filters.hashCode() + '-' + #pageable.pageNumber")
    public Page<MeetingReportRecordDto> getReportRecordsForStream(
            UUID streamId,
            List<Filter> filters,
            Pageable pageable
    ) {
        Sort effectiveSort = calculateEffectiveSort(pageable.getSort());
        Specification<Meeting> baseSpec = buildBaseSpec(streamId, filters);

        Page<Meeting> meetingPage = meetingRepository.findAll(baseSpec,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), effectiveSort));

        if (meetingPage.isEmpty()) return Page.empty(pageable);

        List<MeetingReportRecordDto> dtos = meetingPage.getContent().stream()
                .map(meetingMapper::mapToReportDto)
                .toList();

        return new PageImpl<>(dtos, pageable, meetingPage.getTotalElements());
    }

    @Override
    public void streamRecordsToExcelForStream(
            UUID streamId,
            List<Filter> filters,
            Sort sort,
            int fetchPageSize,
            int exportLimit,
            OutputStream outputStream
    ) throws IOException {
        Specification<Meeting> baseSpec = buildBaseSpec(streamId, filters);
        String streamName = streamId.toString();
        Sort effectiveSort = calculateEffectiveSort(sort);

        var recordStream = IntStream.iterate(0, i -> i + 1)
                .mapToObj(page -> {
                    Pageable pageable = PageRequest.of(page, fetchPageSize, effectiveSort);

                    var meetingPage = meetingRepository.findAll(baseSpec, pageable);
                    if (meetingPage.isEmpty()) return List.<MeetingReportRecordDto>of();

                    return meetingPage.getContent().stream()
                            .map(meetingMapper::mapToReportDto)
                            .toList();
                })
                .takeWhile(batch -> !batch.isEmpty())
                .flatMap(Collection::stream)
                .limit(exportLimit);

        excelGenerator.generate(streamName, recordStream, outputStream);
    }

    private Sort calculateEffectiveSort(Sort clientSort) {
        if (clientSort.getOrderFor(DEFAULT_GROUP_BY_SORT_FIELD) != null) {
            return clientSort;
        }
        if (clientSort.isUnsorted()) {
            return DEFAULT_SORT;
        }
        return DEFAULT_GROUP_BY_SORT.and(clientSort);
    }

    private Specification<Meeting> buildBaseSpec(UUID streamId, List<Filter> filters) {
        return belongsToStream(streamId).and(withFilters(filters));
    }
}

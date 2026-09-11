package net.trackme.meetingservice.services.report;

import lombok.RequiredArgsConstructor;
import net.trackme.commons.filters.Filter;
import net.trackme.meetingservice.api.dto.TrackerMeetingReportRecordDto;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.mapping.MeetingMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import static net.trackme.meetingservice.entities.MeetingSpecification.*;

@Service
@RequiredArgsConstructor
public class TrackerMeetingsReportServiceImpl implements TrackerMeetingsReportService {

    private static final String DEFAULT_GROUP_BY_SORT_FIELD = "teamName";
    private static final Sort.Direction DEFAULT_GROUP_BY_SORT_DIRECTION = Sort.Direction.ASC;
    private static final Sort DEFAULT_GROUP_BY_SORT = Sort.by(
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
    private final TrackerMeetingsReportExcelGenerator excelGenerator;

    @Override
    @Cacheable(value = "tracker-meetings-report-all-my",
               key = "#filters.hashCode() + '-' + authentication.name")
    public List<TrackerMeetingReportRecordDto> getReportRecordsForTracker(List<Filter> filters) {
        var username = SecurityContextHolder.getContext().getAuthentication().getName();
        Specification<Meeting> spec = withFilters(filters)
                .and(trackerUsernameEquals(username));
        return meetingRepository.findAll(spec, DEFAULT_SORT).stream()
                .map(meetingMapper::mapToTrackerReportDto).toList();
    }

    @Override
    @Cacheable(value = "tracker-meetings-report-page-my",
               key = "#filters.hashCode() + '-' + #pageable.pageNumber + '-' + authentication.name")
    public Page<TrackerMeetingReportRecordDto> getReportRecordsForTracker(
            List<Filter> filters, Pageable pageable) {
        var username = SecurityContextHolder.getContext().getAuthentication().getName();
        Sort effectiveSort = calculateEffectiveSort(pageable.getSort());
        Specification<Meeting> baseSpec = withFilters(filters)
                .and(trackerUsernameEquals(username));
        Page<Meeting> meetingPage = meetingRepository.findAll(baseSpec,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), effectiveSort));
        if (meetingPage.isEmpty()) return Page.empty(pageable);
        List<TrackerMeetingReportRecordDto> dtos = meetingPage.getContent().stream()
                .map(meetingMapper::mapToTrackerReportDto).toList();
        return new PageImpl<>(dtos, pageable, meetingPage.getTotalElements());
    }

    @Override
    public void streamRecordsToExcel(
            List<Filter> filters, Sort sort,
            int fetchPageSize, int exportLimit, OutputStream outputStream) throws IOException {
        var username = SecurityContextHolder.getContext().getAuthentication().getName();
        Specification<Meeting> baseSpec = withFilters(filters)
                .and(trackerUsernameEquals(username));
        Sort effectiveSort = calculateEffectiveSort(sort);

        var recordStream = IntStream.iterate(0, i -> i + 1)
                .mapToObj(page -> {
                    Pageable pageable = PageRequest.of(page, fetchPageSize, effectiveSort);
                    var meetingPage = meetingRepository.findAll(baseSpec, pageable);
                    if (meetingPage.isEmpty()) return List.<TrackerMeetingReportRecordDto>of();
                    return meetingPage.getContent().stream()
                            .map(meetingMapper::mapToTrackerReportDto).toList();
                })
                .takeWhile(batch -> !batch.isEmpty())
                .flatMap(Collection::stream)
                .limit(exportLimit);

        excelGenerator.generate("tracker-report", recordStream, outputStream);
    }

    private Sort calculateEffectiveSort(Sort clientSort) {
        if (clientSort.getOrderFor(DEFAULT_GROUP_BY_SORT_FIELD) != null) return clientSort;
        if (clientSort.isUnsorted()) return DEFAULT_SORT;
        return DEFAULT_GROUP_BY_SORT.and(clientSort);
    }
}

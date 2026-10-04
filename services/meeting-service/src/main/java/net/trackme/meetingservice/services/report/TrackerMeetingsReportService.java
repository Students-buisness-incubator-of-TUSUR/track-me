package net.trackme.meetingservice.services.report;

import net.trackme.commons.filters.Filter;
import net.trackme.meetingservice.api.dto.TrackerMeetingReportRecordDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public interface TrackerMeetingsReportService {

    List<TrackerMeetingReportRecordDto> getReportRecordsForTracker(
        List<Filter> filters
    );

    Page<TrackerMeetingReportRecordDto> getReportRecordsForTracker(
        List<Filter> filters,
        Pageable pageable
    );

    void streamRecordsToExcel(
        List<Filter> filters,
        Sort sort,
        int fetchPageSize,
        int exportLimit,
        OutputStream outputStream
    ) throws IOException;
}

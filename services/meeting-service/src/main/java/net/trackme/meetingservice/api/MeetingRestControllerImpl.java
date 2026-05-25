package net.trackme.meetingservice.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.commons.filters.FilterRequest;
import net.trackme.meetingservice.api.dto.MeetingCreateDto;
import net.trackme.meetingservice.api.dto.MeetingDto;
import net.trackme.meetingservice.api.dto.MeetingReportRecordDto;
import net.trackme.meetingservice.api.dto.MeetingUpdateDto;
import net.trackme.meetingservice.services.MeetingService;
import net.trackme.meetingservice.services.exceptions.MeetingExcelReportException;
import net.trackme.meetingservice.services.report.MeetingsReportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@Slf4j
@RequiredArgsConstructor
public class MeetingRestControllerImpl implements MeetingRestController {

    private final MeetingService meetingService;
    private final MeetingsReportService meetingsReportService;

    @Override
    public ResponseEntity<MeetingDto> createMeeting(UUID teamCardId,
                                                    MeetingCreateDto meetingCreateDto) {
        var meeting = meetingService.createMeeting(teamCardId, meetingCreateDto);
        return ResponseEntity.ok(meeting);
    }

    @Override
    public PagedModel<MeetingDto> getMeetings(UUID teamCardId, Pageable pageable) {
        var meetings = meetingService.getMeetings(teamCardId, pageable);
        return new PagedModel<>(meetings);
    }

    @Override
    public ResponseEntity<MeetingDto> updateMeeting(UUID meetingId, UUID teamCardId,
                                                    MeetingUpdateDto meetingCreateDto) {
        var meeting = meetingService.updateMeeting(meetingId, teamCardId, meetingCreateDto);
        return ResponseEntity.ok(meeting);
    }

    @Override
    public ResponseEntity<Void> deleteMeeting(UUID meetingId) {
        meetingService.deleteMeeting(meetingId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> addImage(UUID meetingId, MultipartFile file) {
        meetingService.addMeetingImage(meetingId, file);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Resource> getImage(UUID meetingId) {
        var image = meetingService.getMeetingImage(meetingId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }

    @Override
    public ResponseEntity<PagedModel<MeetingReportRecordDto>> getMeetingsReport(
            UUID streamId,
            FilterRequest filters,
            Pageable pageable
    ) {
        var page = meetingsReportService.getReportRecordsForStream(streamId, filters.filters(), pageable);
        return ResponseEntity.ok(new PagedModel<>(page));
    }

    @Value("${app.report.export-limit:10000}")
    private int exportLimit;

    @Value("${app.report.fetch-page-size:500}")
    private int fetchPageSize;

    @Override
    public ResponseEntity<StreamingResponseBody> getMeetingsReportExcel(
            UUID streamId,
            FilterRequest filters,
            Pageable pageable
    ) {
        String filename = "отчёт-по-встречам-" +
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) +
            ".xlsx";

        ContentDisposition contentDisposition = ContentDisposition.attachment()
            .filename(filename, StandardCharsets.UTF_8)
            .build();

        StreamingResponseBody responseBody = outputStream -> {
            try {
                meetingsReportService.streamRecordsToExcelForStream(
                    streamId,
                    filters.filters(),
                    pageable.getSort(),
                    fetchPageSize,
                    exportLimit,
                    outputStream
                );
            } catch (Exception e) {
                log.error("Error during Excel streaming for streamId={}", streamId, e);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    contentDisposition.toString()
                )
                .contentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .body(responseBody);
    }

    // НОВЫЙ МЕТОД ДЛЯ СУПЕРАДМИНИСТРАТОРА
    @Override
    public ResponseEntity<MeetingDto> updateBySuperAdmin(UUID meetingId,
        MeetingUpdateDto updateDto) {
        return ResponseEntity.ok(meetingService.updateBySuperAdmin(meetingId, updateDto));
    }
}

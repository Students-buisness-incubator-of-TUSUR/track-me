package net.trackme.meetingservice.services.report;

import net.trackme.commons.filters.Filter;
import net.trackme.commons.filters.OperationType;
import net.trackme.meetingservice.api.dto.MeetingReportRecordDto;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.mapping.MeetingMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingsReportServiceImplTest {

    @Mock private MeetingRepository meetingRepository;
    @Mock private MeetingMapper meetingMapper;
    @Mock private MeetingsReportExcelGenerator excelGenerator;

    @InjectMocks
    private MeetingsReportServiceImpl reportService;

    @Test
    void getReportRecordsForStream_List_WithFilters_ReturnsData() {
        UUID streamId = UUID.randomUUID();
        Filter teamFilter = Filter.builder()
                .fieldName("teamName")
                .type(OperationType.EQUALS)
                .singleValue("Alpha")
                .build();

        List<Filter> filters = List.of(teamFilter);
        Meeting meeting = new Meeting();
        meeting.setId(UUID.randomUUID());

        when(meetingRepository.findAll(ArgumentMatchers.<Specification<Meeting>>any(), any(Sort.class)))
                .thenReturn(List.of(meeting));
        when(meetingMapper.mapToReportDto(any())).thenReturn(MeetingReportRecordDto.builder().teamName("Alpha").build());

        List<MeetingReportRecordDto> result = reportService.getReportRecordsForStream(streamId, filters);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(meetingRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void getReportRecordsForStream_Page_WithFilters_ReturnsPagedData() {
        UUID streamId = UUID.randomUUID();
        Filter statusFilter = new Filter("status", OperationType.EQUALS, null, "COMPLETED");
        List<Filter> filters = List.of(statusFilter);
        Pageable pageable = Pageable.ofSize(10);

        when(meetingRepository.findAll(ArgumentMatchers.<Specification<Meeting>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new Meeting())));
        when(meetingRepository.findAll(ArgumentMatchers.<Specification<Meeting>>any(), any(Sort.class)))
                .thenReturn(List.of(new Meeting()));
        when(meetingMapper.mapToReportDto(any())).thenReturn(MeetingReportRecordDto.builder().build());

        Page<MeetingReportRecordDto> result = reportService.getReportRecordsForStream(streamId, filters, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getReportRecordsForStream_WithCustomSort_PreservesClientSort() {
        UUID streamId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("teamName").descending());
        Meeting meeting = new Meeting();
        meeting.setId(UUID.randomUUID());

        when(meetingRepository.findAll(ArgumentMatchers.<Specification<Meeting>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(meeting)));
        when(meetingRepository.findAll(ArgumentMatchers.<Specification<Meeting>>any(), any(Sort.class)))
                .thenReturn(List.of(meeting));
        when(meetingMapper.mapToReportDto(any())).thenReturn(MeetingReportRecordDto.builder().build());

        reportService.getReportRecordsForStream(streamId, List.of(), pageable);

        verify(meetingRepository).findAll(any(Specification.class), argThat((Pageable p) ->
                p.getSort().getOrderFor("teamName").getDirection() == Sort.Direction.DESC
        ));
    }

    @Test
    void getReportRecordsForStream_List_ReturnsData() {
        UUID streamId = UUID.randomUUID();
        Meeting meeting = new Meeting();
        meeting.setId(UUID.randomUUID());
        MeetingReportRecordDto dto = MeetingReportRecordDto.builder().teamName("Team").build();

        when(meetingRepository.findAll(ArgumentMatchers.<Specification<Meeting>>any(), any(Sort.class)))
                .thenReturn(List.of(meeting));
        when(meetingMapper.mapToReportDto(any(Meeting.class))).thenReturn(dto);

        List<MeetingReportRecordDto> result = reportService.getReportRecordsForStream(streamId, List.of());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Team", result.get(0).teamName());
    }

    @Test
    void getReportRecordsForStream_Page_ReturnsPagedData() {
        UUID streamId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Meeting meeting = new Meeting();
        meeting.setId(UUID.randomUUID());
        MeetingReportRecordDto dto = MeetingReportRecordDto.builder().teamName("Team Page").build();

        when(meetingRepository.findAll(ArgumentMatchers.<Specification<Meeting>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(meeting), pageable, 1));

        when(meetingRepository.findAll(ArgumentMatchers.<Specification<Meeting>>any(), any(Sort.class)))
                .thenReturn(List.of(meeting));

        when(meetingMapper.mapToReportDto(any(Meeting.class))).thenReturn(dto);

        Page<MeetingReportRecordDto> result = reportService.getReportRecordsForStream(streamId, List.of(), pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Team Page", result.getContent().get(0).teamName());
    }

    @Test
    void getReportRecordsForStream_Page_ReturnsEmptyWhenNoData() {
        UUID streamId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        when(meetingRepository.findAll(ArgumentMatchers.<Specification<Meeting>>any(), any(Pageable.class)))
                .thenReturn(Page.empty(pageable));

        Page<MeetingReportRecordDto> result = reportService.getReportRecordsForStream(streamId, List.of(), pageable);

        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
        verify(meetingRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
        verify(meetingRepository, never()).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void streamRecordsToExcel_fetchesPagesUntilEmpty() throws Exception {
        UUID streamId = UUID.randomUUID();
        int fetchPageSize = 2;
        int exportLimit = 10;
        var out = new ByteArrayOutputStream();

        Meeting meeting1 = new Meeting(); meeting1.setId(UUID.randomUUID());
        Meeting meeting2 = new Meeting(); meeting2.setId(UUID.randomUUID());

        doAnswer(invocation -> {
            Stream<MeetingReportRecordDto> stream = invocation.getArgument(1);
            stream.forEach(record -> {});
            return null;
        }).when(excelGenerator).generate(anyString(), any(), any());

        when(meetingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(meeting1, meeting2)))
                .thenReturn(new PageImpl<>(List.of()));

        when(meetingRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(meeting1, meeting2));

        when(meetingMapper.mapToReportDto(any(Meeting.class)))
                .thenReturn(MeetingReportRecordDto.builder().teamName("Test").build());

        reportService.streamRecordsToExcelForStream(streamId, List.of(), Sort.unsorted(), fetchPageSize, exportLimit, out);

        verify(excelGenerator, times(1)).generate(eq(streamId.toString()), any(), eq(out));
        verify(meetingRepository, atLeastOnce()).findAll(any(Specification.class), any(Pageable.class));
    }
}
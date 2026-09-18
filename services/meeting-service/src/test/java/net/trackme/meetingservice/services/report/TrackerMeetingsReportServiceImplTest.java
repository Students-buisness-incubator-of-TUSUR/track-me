package net.trackme.meetingservice.services.report;

import net.trackme.commons.filters.Filter;
import net.trackme.commons.filters.OperationType;
import net.trackme.meetingservice.api.dto.TrackerMeetingReportRecordDto;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.entities.TeamStatus;
import net.trackme.meetingservice.mapping.MeetingMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackerMeetingsReportServiceImplTest {

    @Mock private MeetingRepository meetingRepository;
    @Mock private MeetingMapper meetingMapper;
    @Mock private TrackerMeetingsReportExcelGenerator excelGenerator;

    @InjectMocks
    private TrackerMeetingsReportServiceImpl reportService;

    private static final String TRACKER_USERNAME = "tracker1";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String username) {
        var auth = new UsernamePasswordAuthenticationToken(username, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private TrackerMeetingReportRecordDto sampleDto(String teamName) {
        return TrackerMeetingReportRecordDto.builder()
                .teamId(UUID.randomUUID())
                .teamName(teamName)
                .startDate(OffsetDateTime.now())
                .tasksNextMeeting("next")
                .tasksCurrentMeeting("current")
                .teamStatus(TeamStatus.OK)
                .status(MeetingStatus.COMPLETED)
                .build();
    }

    // ========== getReportRecordsForTracker(List<Filter>) ==========

    @Test
    void getReportRecordsForTracker_List_ReturnsDataForCurrentTracker() {
        authenticateAs(TRACKER_USERNAME);
        var meeting = new Meeting();
        meeting.setId(UUID.randomUUID());
        meeting.setTrackerUsername(TRACKER_USERNAME);

        when(meetingRepository.findAll(ArgumentMatchers.<Specification<Meeting>>any(), any(Sort.class)))
                .thenReturn(List.of(meeting));
        when(meetingMapper.mapToTrackerReportDto(any(Meeting.class)))
                .thenReturn(sampleDto("Alpha"));

        var result = reportService.getReportRecordsForTracker(List.of());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Alpha", result.get(0).teamName());
        verify(meetingRepository, times(1))
                .findAll(ArgumentMatchers.<Specification<Meeting>>any(), any(Sort.class));
        verify(meetingMapper, times(1)).mapToTrackerReportDto(meeting);
    }

    @Test
    void getReportRecordsForTracker_List_WithFilters_AppliesFiltersAndTrackerPredicate() {
        authenticateAs(TRACKER_USERNAME);
        var filter = Filter.builder()
                .fieldName("teamName")
                .type(OperationType.EQUALS)
                .singleValue("Alpha")
                .build();

        when(meetingRepository.findAll(ArgumentMatchers.<Specification<Meeting>>any(), any(Sort.class)))
                .thenReturn(List.of());

        var result = reportService.getReportRecordsForTracker(List.of(filter));

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(meetingRepository, times(1))
                .findAll(ArgumentMatchers.<Specification<Meeting>>any(), any(Sort.class));
    }

    @Test
    void getReportRecordsForTracker_List_EmptyResult() {
        authenticateAs(TRACKER_USERNAME);
        when(meetingRepository.findAll(ArgumentMatchers.<Specification<Meeting>>any(), any(Sort.class)))
                .thenReturn(List.of());

        var result = reportService.getReportRecordsForTracker(List.of());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== getReportRecordsForTracker(List<Filter>, Pageable) ==========

    @Test
    void getReportRecordsForTracker_Page_ReturnsPagedData() {
        authenticateAs(TRACKER_USERNAME);
        Pageable pageable = PageRequest.of(0, 10);
        var meeting = new Meeting();
        meeting.setId(UUID.randomUUID());

        when(meetingRepository.findAll(
                ArgumentMatchers.<Specification<Meeting>>any(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(meeting), pageable, 1));
        when(meetingMapper.mapToTrackerReportDto(any(Meeting.class)))
                .thenReturn(sampleDto("Alpha"));

        var result = reportService.getReportRecordsForTracker(List.of(), pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Alpha", result.getContent().get(0).teamName());
        verify(meetingRepository, times(1))
                .findAll(ArgumentMatchers.<Specification<Meeting>>any(), any(Pageable.class));
    }

    @Test
    void getReportRecordsForTracker_Page_EmptyPage_PreservesTotalElements() {
        authenticateAs(TRACKER_USERNAME);
        // Запрашиваем страницу за пределами (page=999), но всего 100 записей
        Pageable pageable = PageRequest.of(999, 20);

        when(meetingRepository.findAll(
                ArgumentMatchers.<Specification<Meeting>>any(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 100));

        var result = reportService.getReportRecordsForTracker(List.of(), pageable);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        // ⚠️ КРИТИЧНО: totalElements должен быть 100, а не 0
        assertEquals(100, result.getTotalElements(),
                "totalElements must be preserved even for empty page beyond last");
    }

    @Test
    void getReportRecordsForTracker_Page_TrulyEmptyRepository() {
        authenticateAs(TRACKER_USERNAME);
        Pageable pageable = PageRequest.of(0, 10);

        when(meetingRepository.findAll(
                ArgumentMatchers.<Specification<Meeting>>any(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var result = reportService.getReportRecordsForTracker(List.of(), pageable);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getReportRecordsForTracker_Page_CustomSort_PreservesClientSort() {
        authenticateAs(TRACKER_USERNAME);
        // Клиент сортирует по teamName → должен сохраниться
        Pageable pageable = PageRequest.of(0, 10, Sort.by("teamName").descending());

        when(meetingRepository.findAll(
                ArgumentMatchers.<Specification<Meeting>>any(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        reportService.getReportRecordsForTracker(List.of(), pageable);

        verify(meetingRepository).findAll(
                ArgumentMatchers.<Specification<Meeting>>any(),
                argThat((Pageable p) -> {
                    var order = p.getSort().getOrderFor("teamName");
                    return order != null
                            && order.getDirection() == Sort.Direction.DESC;
                }));
    }

    @Test
    void getReportRecordsForTracker_Page_UnsortedRequest_AppliesDefaultSort() {
        authenticateAs(TRACKER_USERNAME);
        Pageable pageable = PageRequest.of(0, 10, Sort.unsorted());

        when(meetingRepository.findAll(
                ArgumentMatchers.<Specification<Meeting>>any(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        reportService.getReportRecordsForTracker(List.of(), pageable);

        verify(meetingRepository).findAll(
                ArgumentMatchers.<Specification<Meeting>>any(),
                argThat((Pageable p) -> {
                    // DEFAULT_SORT = teamName ASC + startDate DESC
                    var teamNameOrder = p.getSort().getOrderFor("teamName");
                    var startDateOrder = p.getSort().getOrderFor("startDate");
                    return teamNameOrder != null
                            && startDateOrder != null
                            && teamNameOrder.getDirection() == Sort.Direction.ASC
                            && startDateOrder.getDirection() == Sort.Direction.DESC;
                }));
    }

    @Test
    void getReportRecordsForTracker_Page_SortByOtherField_AppliesGroupByFirst() {
        authenticateAs(TRACKER_USERNAME);
        // Клиент сортирует по startDate → GROUP_BY (teamName) должен быть добавлен
        Pageable pageable = PageRequest.of(0, 10, Sort.by("startDate").descending());

        when(meetingRepository.findAll(
                ArgumentMatchers.<Specification<Meeting>>any(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        reportService.getReportRecordsForTracker(List.of(), pageable);

        verify(meetingRepository).findAll(
                ArgumentMatchers.<Specification<Meeting>>any(),
                argThat((Pageable p) -> {
                    var teamNameOrder = p.getSort().getOrderFor("teamName");
                    var startDateOrder = p.getSort().getOrderFor("startDate");
                    return teamNameOrder != null
                            && startDateOrder != null
                            && teamNameOrder.getDirection() == Sort.Direction.ASC
                            && startDateOrder.getDirection() == Sort.Direction.DESC;
                }));
    }

    // ========== streamRecordsToExcel ==========

    @Test
    void streamRecordsToExcel_fetchesPagesUntilEmpty() throws Exception {
        authenticateAs(TRACKER_USERNAME);
        int fetchPageSize = 2;
        int exportLimit = 10;
        var out = new ByteArrayOutputStream();

        var meeting1 = new Meeting(); meeting1.setId(UUID.randomUUID());
        var meeting2 = new Meeting(); meeting2.setId(UUID.randomUUID());

        doAnswer(invocation -> {
            // Проходимся по стриму — так же, как это делает генератор
            java.util.stream.Stream<TrackerMeetingReportRecordDto> stream = invocation.getArgument(0);
            stream.forEach(dto -> {});
            return null;
        }).when(excelGenerator).generate(any(), eq(out));

        when(meetingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(meeting1, meeting2)))
                .thenReturn(new PageImpl<>(List.of()));

        when(meetingMapper.mapToTrackerReportDto(any(Meeting.class)))
                .thenReturn(sampleDto("Alpha"));

        reportService.streamRecordsToExcel(List.of(), Sort.unsorted(), fetchPageSize, exportLimit, out);

        verify(excelGenerator, times(1)).generate(any(), eq(out));
        // Должно быть минимум 2 вызова: первая непустая страница + вторая пустая (takeWhile)
        verify(meetingRepository, atLeast(2))
                .findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void streamRecordsToExcel_respectsExportLimit() throws Exception {
        authenticateAs(TRACKER_USERNAME);
        int fetchPageSize = 2;
        int exportLimit = 3; // меньше, чем всего записей
        var out = new ByteArrayOutputStream();

        // Готовим 3 встречи (2 + 1)
        var meeting1 = new Meeting(); meeting1.setId(UUID.randomUUID());
        var meeting2 = new Meeting(); meeting2.setId(UUID.randomUUID());
        var meeting3 = new Meeting(); meeting3.setId(UUID.randomUUID());

        when(meetingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(meeting1, meeting2)))
                .thenReturn(new PageImpl<>(List.of(meeting3)))
                .thenReturn(new PageImpl<>(List.of()));

        when(meetingMapper.mapToTrackerReportDto(any(Meeting.class)))
                .thenReturn(sampleDto("Alpha"));

        // Захватываем, сколько DTO прошло через стрим
        var processedCount = new int[]{0};
        doAnswer(invocation -> {
            java.util.stream.Stream<TrackerMeetingReportRecordDto> stream = invocation.getArgument(0);
            processedCount[0] = (int) stream.count();
            return null;
        }).when(excelGenerator).generate(any(), eq(out));

        reportService.streamRecordsToExcel(List.of(), Sort.unsorted(), fetchPageSize, exportLimit, out);

        assertEquals(exportLimit, processedCount[0],
                "streamRecordsToExcel must respect exportLimit");
    }

    @Test
    void streamRecordsToExcel_emptyRepository_stopsImmediately() throws Exception {
        authenticateAs(TRACKER_USERNAME);
        var out = new ByteArrayOutputStream();

        when(meetingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        doAnswer(invocation -> {
            java.util.stream.Stream<TrackerMeetingReportRecordDto> stream = invocation.getArgument(0);
            assertEquals(0, stream.count());
            return null;
        }).when(excelGenerator).generate(any(), eq(out));

        reportService.streamRecordsToExcel(List.of(), Sort.unsorted(), 10, 100, out);

        verify(meetingRepository, times(1))
                .findAll(any(Specification.class), any(Pageable.class));
        verify(excelGenerator, times(1)).generate(any(), eq(out));
    }
}

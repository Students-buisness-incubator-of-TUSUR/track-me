package net.trackme.meetingservice.services.report;

import net.trackme.meetingservice.api.dto.MeetingReportRecordDto;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.mapping.MeetingMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

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

        // Act
        reportService.streamRecordsToExcelForStream(streamId, List.of(), Sort.unsorted(), fetchPageSize, exportLimit, out);

        // Assert
        verify(excelGenerator, times(1)).generate(eq(streamId.toString()), any(), eq(out));

        // Теперь верификация пройдет, так как doAnswer заставил стрим выполниться
        verify(meetingRepository, atLeastOnce()).findAll(any(Specification.class), any(Pageable.class));
    }
}
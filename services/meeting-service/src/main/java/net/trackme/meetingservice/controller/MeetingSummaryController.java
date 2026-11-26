package net.trackme.meetingservice.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import net.trackme.meetingservice.messaging.own.MeetingSummaryEvent;
import net.trackme.meetingservice.services.MeetingSummaryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MeetingSummaryController {
    private final MeetingSummaryService summaries;

    @GetMapping("/api/v1/admin/reports/missed-meetings")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public List<MeetingSummaryEvent> currentMissedMeetings() {
        return summaries.currentMissedMeetings();
    }
}

package net.trackme.sso.services;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;

public interface NotificationService {
    void sendMeetingNotHappenedNotification(String teamCardUsername,
                                            String teamCardName,
                                            String streamName,
                                            String meetingLink,
                                            String trackerFullName);

    void sendTeamCardSummary(List<LinkedHashMap<String, String>> teamCardSummaryEvents);

    void sendTeamCardLowGradeSummary(List<LinkedHashMap<String, String>> teamCardSummaryEvents);
    
    void sendMeetingInvite(
            String email,
            String fullName,
            String teamName,
            String meetingLink,
            OffsetDateTime meetingDate);

    void sendMeetingReminder(
            String email,
            String fullName,
            String teamName,
            String meetingLink,
            OffsetDateTime meetingDate);

    void sendMeetingInviteByUsername(
            String username,
            String teamName,
            String meetingLink,
            OffsetDateTime meetingDate);

    void sendMeetingReminderByUsername(
            String username,
            String teamName,
            String meetingLink,
            OffsetDateTime meetingDate,
            int daysUntilMeeting);
}

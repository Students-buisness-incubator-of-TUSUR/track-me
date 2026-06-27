package net.trackme.sso.services;

import java.util.List;
import java.util.Map;

public interface NotificationService {
    void sendMeetingNotHappenedNotification(String teamCardUsername,
                                            String teamCardName,
                                            String streamName,
                                            String meetingLink,
                                            String trackerFullName);

    void sendTeamCardSummary(List<Map<String, String>> teamCardSummaryEvents);

    void sendTeamCardLowGradeSummary(List<Map<String, String>> teamCardSummaryEvents);
}

package net.trackme.sso.services;

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
}

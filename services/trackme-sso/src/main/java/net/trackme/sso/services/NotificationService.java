package net.trackme.sso.services;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface NotificationService {
    void sendMeetingNotHappenedNotification(String teamCardUsername,
                                            String teamCardName,
                                            String streamName,
                                            String meetingLink,
                                            String trackerFullName);

    void sendTeamCardSummary(List<LinkedHashMap<String, String>> teamCardSummaryEvents);

    void sendTeamCardLowGradeSummary(List<LinkedHashMap<String, String>> teamCardSummaryEvents);

    void sendMeetingEmail(String email,
                          String fullName,
                          String teamName,
                          String meetingLink,
                          OffsetDateTime meetingDate,
                          String templateName,
                          String subject,
                          Map<String, Object> extraParams);

    void sendMeetingEmailByUsername(String username,
                                    String teamName,
                                    String meetingLink,
                                    OffsetDateTime meetingDate,
                                    String templateName,
                                    String subject,
                                    Map<String, Object> extraParams);
}

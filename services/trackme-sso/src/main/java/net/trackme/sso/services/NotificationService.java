package net.trackme.sso.services;

import java.time.OffsetDateTime;
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

    void sendMeetingEmail(EmailRecipient recipient,
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

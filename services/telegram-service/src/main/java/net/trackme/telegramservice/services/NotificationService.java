package net.trackme.telegramservice.services;

import java.math.BigDecimal;

public interface NotificationService {
    void sendMeetingNotHappenedMessage(String username,
                                       String meetingLink,
                                       BigDecimal averageGrade);
}

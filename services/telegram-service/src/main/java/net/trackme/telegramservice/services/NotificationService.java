package net.trackme.telegramservice.services;

public interface NotificationService {
    /**
     * Отправить сообщение о пропущенной встрече.
     * @param teamCardUsername Имя пользователя
     * @param teamCardName Название команды
     * @param streamName Название потока команды
     * @param meetingLink Ссылка на встречу
     */
    void sendMeetingNotHappenedMessage(String teamCardUsername,
                                       String teamCardName,
                                       String streamName,
                                       String meetingLink);
}

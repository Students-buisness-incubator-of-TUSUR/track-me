package net.trackme.telegramservice.services;

public interface NotificationService {
    /**
     * Отправить сообщение о пропущенной встрече.
     * @param teamCardUsername Имя пользователя
     * @param teamCardName Название команды
     * @param streamName Название потока команды
     * @param meetingLink Ссылка на встречу
     * @param trackerFullName ФИО трекера
     */
    void sendMeetingNotHappenedMessage(String teamCardUsername,
                                       String teamCardName,
                                       String streamName,
                                       String meetingLink,
                                       String trackerFullName);
}
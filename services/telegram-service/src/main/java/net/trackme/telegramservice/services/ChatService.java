package net.trackme.telegramservice.services;

public interface ChatService {
    /**
     * Создать чат.
     * @param chatId Идентификатор чата в Telegram
     * @param username Имя пользователя
     */
    void createChat(Long chatId, String username);
}

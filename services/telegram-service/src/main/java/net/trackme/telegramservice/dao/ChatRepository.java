package net.trackme.telegramservice.dao;

import net.trackme.telegramservice.entities.ChatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ChatRepository extends JpaRepository<ChatEntity, UUID>,
        JpaSpecificationExecutor<ChatEntity> {
    /**
     * Найти чат по имени пользователя.
     * @param username Имя пользователя
     * @return Чат
     */
    ChatEntity findByUsername(String username);
}

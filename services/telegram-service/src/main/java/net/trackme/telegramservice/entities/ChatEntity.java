package net.trackme.telegramservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@Table(name = "chats")
@AllArgsConstructor
@NoArgsConstructor
public class ChatEntity {
    /**
     * Идентификатор.
     */
    @Id
    @Column(
            nullable = false,
            updatable = false)
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    /**
     * Идентификатор чата.
     */
    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    /**
     * Имя пользователя.
     */
    @Column(nullable = false)
    private String username;
}

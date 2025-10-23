package net.trackme.telegramservice.entities;

import jakarta.persistence.*;
import lombok.*;
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
    @Id
    @Column(
            nullable = false,
            updatable = false)
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(nullable = false)
    private String username;
}

package net.trackme.telegramservice.services;

import net.trackme.telegramservice.dao.ChatRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ChatServiceImplTest extends AbstractIntegrationTest {

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private ChatService chatService;

    @Test
    void createChat_success() {
        // Arrange
        Long chatId = 1L;
        String username = "Test username";

        // Act
        chatService.createChat(chatId, username);

        // Assert
        var expectedChat = chatRepository.findByUsername(username);
        Assertions.assertEquals(chatId, expectedChat.getChatId());
        Assertions.assertEquals(username, expectedChat.getUsername());
    }
}
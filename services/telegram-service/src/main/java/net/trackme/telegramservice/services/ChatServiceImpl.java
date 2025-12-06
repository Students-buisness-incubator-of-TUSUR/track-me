package net.trackme.telegramservice.services;

import lombok.RequiredArgsConstructor;
import net.trackme.telegramservice.dao.ChatRepository;
import net.trackme.telegramservice.entities.ChatEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    /**
     * Репозиторий чатов.
     */
    private final ChatRepository chatRepository;

    @Override
    public void createChat(Long chatId, String username) {
        Assert.notNull(chatId, "Chat Id must not be null");
        Assert.notNull(username, "Username must not be null");
        Assert.hasText(username, "Username must not be empty");

        ChatEntity chat = chatRepository.findByUsername(username);

        if (chat == null) {
            chat = new ChatEntity();
            chat.setChatId(chatId);
            chat.setUsername(username);
            chatRepository.save(chat);
        }
    }
}

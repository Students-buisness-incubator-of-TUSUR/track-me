package net.trackme.sso.controller;

import jakarta.mail.internet.MimeMessage;
import net.trackme.sso.AbstractIntegrationTest;
import net.trackme.sso.components.impl.RedisRegistrationStore;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderValidatorAutoConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ImportAutoConfiguration(exclude = {
        MailSenderAutoConfiguration.class,
        MailSenderValidatorAutoConfiguration.class
})
class RegistrationControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private JavaMailSender javaMailSender;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void testRegistration_success() throws Exception {
        // Stub createMimeMessage to return a non-null instance
        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Перед запросом запоминаем ключи
        Set<String> keysBefore = stringRedisTemplate.keys(RedisRegistrationStore.SESSION_ID_TO_REG_DATA + "*");

        // Выполняем регистрацию
        mockMvc.perform(post("/api/v1/registration/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "johndoe",
                                    "password": "password123",
                                    "phoneNumber": "+79001234567",
                                    "fullName": "John Doe",
                                    "email": "john@john.john",
                                    "role": "USER"
                                }
                                """)
                        .with(csrf()))
                .andExpect(status().isOk());

        // Проверяем, что отправка письма действительно произошла
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));

        // Проверяем, что после запроса в Redis появился хотя бы один новый ключ
        Set<String> keysAfter = stringRedisTemplate.keys(RedisRegistrationStore.SESSION_ID_TO_REG_DATA + "*");
        Assertions.assertThat(keysAfter)
                .as("Ожидается появление хотя бы одной новой записи в Redis")
                .isNotNull()
                .isNotEmpty();

        // Сравниваем с ключами до выполнения запроса, чтобы найти новые
        keysAfter.removeAll(keysBefore);
        Assertions.assertThat(keysAfter)
                .as("Ожидается, что появился новый ключ в Redis после регистрации")
                .hasSizeGreaterThanOrEqualTo(1);
    }

}
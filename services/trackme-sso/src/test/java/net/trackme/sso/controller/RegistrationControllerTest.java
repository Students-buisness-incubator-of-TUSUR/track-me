package net.trackme.sso.controller;

import jakarta.mail.internet.MimeMessage;
import net.trackme.sso.AbstractIntegrationTest;
import net.trackme.sso.components.RegistrationStore;
import net.trackme.sso.components.RegistrationTokenStore;
import net.trackme.sso.components.impl.RedisRegistrationStore;
import net.trackme.sso.dao.repository.UserRepository;
import net.trackme.sso.dto.RecoveryPasswordRequestDto;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RegistrationStore registrationStore;
    @Autowired
    private RegistrationTokenStore registrationTokenStore;

    @BeforeEach
    void setUpUsers()
    {
        var admin = userRepository.findByUsername("superadmin").stream().findFirst().orElseThrow();
        admin.setEmail("superadmin@superadmin.ru");
        userRepository.save(admin);
    }

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
                                    "password": "password123U!",
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

    @Test
    void testRecovery_success() throws Exception {
        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        Set<String> keysBefore = stringRedisTemplate.keys(RedisRegistrationStore.SESSION_ID_TO_REG_DATA + "*");

        mockMvc.perform(post("/api/v1/registration/recovery-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "superadmin@superadmin.ru"
                                }
                                """)
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(javaMailSender, times(1)).send(any(MimeMessage.class));

        Set<String> keysAfter = stringRedisTemplate.keys(RedisRegistrationStore.SESSION_ID_TO_REG_DATA + "*");
        Assertions.assertThat(keysAfter)
                .as("Ожидается появление хотя бы одной новой записи в Redis")
                .isNotNull()
                .isNotEmpty();

        keysAfter.removeAll(keysBefore);
        Assertions.assertThat(keysAfter)
                .as("Ожидается, что появился новый ключ в Redis после регистрации")
                .hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void testRecovery_emailNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/registration/recovery-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "john@john.john"
                                }
                                """)
                        .with(csrf()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testReset_success() throws Exception {
        var requestDto = RecoveryPasswordRequestDto.builder()
                .email("superadmin@superadmin.ru")
                .build();
        var token = registrationTokenStore.generateToken();
        registrationStore.saveToRecovery(requestDto, token.tokenHash());

        mockMvc.perform(post("/api/v1/registration/reset-password")
                        .param("token", token.tokenHash())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "password": "testPassword@123"
                                }
                                """)
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void testReset_tokenIsEmpty() throws Exception {
        mockMvc.perform(post("/api/v1/registration/reset-password")
                        .param("token", "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "password": "testPassword@123"
                                }
                                """)
                        .with(csrf()))
                .andExpect(status().isInternalServerError());
    }
}
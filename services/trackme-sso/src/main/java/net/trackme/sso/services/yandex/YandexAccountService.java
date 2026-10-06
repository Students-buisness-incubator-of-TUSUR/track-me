package net.trackme.sso.services.yandex;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.trackme.sso.dao.entity.UserEntity;
import net.trackme.sso.dao.repository.RoleRepository;
import net.trackme.sso.dao.repository.UserRepository;
import net.trackme.sso.dto.AuthorizedUser;
import net.trackme.sso.mapper.AuthorizedUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class YandexAccountService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwords;

    @Transactional(readOnly = true)
    public Optional<AuthorizedUser> login(YandexProfile profile) {
        profile.requireValid();
        return users.findByYandexId(profile.id()).map(this::principal);
    }

    @Transactional
    public void register(YandexProfile profile, String username, String phone, String role) {
        profile.requireValid();
        if (!Set.of("TRACKER", "ADMIN").contains(role)
                || !username.matches("[a-zA-Z0-9._-]{6,100}")
                || !phone.matches("\\+?\\d{10,15}")) {
            throw new IllegalArgumentException("Проверьте логин, телефон и роль.");
        }
        if (users.findByYandexId(profile.id()).isPresent() || users.existsByUsername(username)
                || users.existsByEmailIgnoreCase(profile.email())) {
            throw new IllegalArgumentException("Аккаунт уже существует. Воспользуйтесь привязкой к существующему аккаунту.");
        }
        var user = new UserEntity();
        user.setUsername(username);
        user.setEmail(profile.email());
        user.setFullName(profile.name());
        user.setPhoneNumber(phone);
        user.setYandexId(profile.id());
        // A non-guessable password hash keeps existing password-based code compatible.
        user.setPasswordHash(passwords.encode(UUID.randomUUID().toString()));
        user.setActive(false);
        user.setAccountNonLocked(true);
        user.getRoles().add(roles.findByCode(role).orElseThrow(
                () -> new IllegalArgumentException("Роль недоступна.")));
        users.saveAndFlush(user);
    }

    @Transactional
    public AuthorizedUser link(YandexProfile profile, String username, String password) {
        profile.requireValid();
        var user = users.findByUsername(username).orElseThrow(YandexAccountService::invalidLogin);
        if (user.getPasswordHash() == null || !passwords.matches(password, user.getPasswordHash())) {
            throw invalidLogin();
        }
        principal(user); // Respect activation and lock state before changing any identity.
        if (user.getYandexId() != null || users.findByYandexId(profile.id()).isPresent()) {
            throw new IllegalArgumentException("Аккаунт уже связан с Яндексом.");
        }
        user.setYandexId(profile.id());
        users.saveAndFlush(user);
        return principal(user);
    }

    private AuthorizedUser principal(UserEntity user) {
        if (!Boolean.TRUE.equals(user.getActive()) || !Boolean.TRUE.equals(user.getAccountNonLocked())) {
            throw new IllegalArgumentException("Аккаунт ожидает активации или заблокирован. Обратитесь к администратору.");
        }
        return AuthorizedUserMapper.map(user);
    }

    private static IllegalArgumentException invalidLogin() {
        return new IllegalArgumentException("Неверные данные аккаунта.");
    }
}

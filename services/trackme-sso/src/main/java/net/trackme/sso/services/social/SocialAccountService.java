package net.trackme.sso.services.social;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.trackme.sso.dao.entity.OrganizationEntity;
import net.trackme.sso.dao.entity.UserEntity;
import net.trackme.sso.dao.repository.OrganizationRepository;
import net.trackme.sso.dao.repository.RoleRepository;
import net.trackme.sso.dao.repository.UserRepository;
import net.trackme.sso.dto.AuthorizedUser;
import net.trackme.sso.mapper.AuthorizedUserMapper;
import net.trackme.sso.messaging.RegistrationRequestedEvent;
import net.trackme.sso.services.CurrentUserService;
import net.trackme.sso.services.OrganizationAccessNotifier;
import net.trackme.sso.services.OrganizationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SocialAccountService {
    private static final String GOOGLE = "google";
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwords;
    private final OrganizationRepository organizations;
    private final OrganizationAccessNotifier accessNotifier;
    private final ApplicationEventPublisher events;

    @Transactional(readOnly = true)
    public Optional<AuthorizedUser> login(SocialProfile profile) {
        profile.requireValid();
        return findIdentity(profile).map(this::principal);
    }

    @Transactional
    public void register(SocialProfile profile, String username, String role) {
        registerAccount(profile, username, role, profile.phoneNumber(), null);
    }

    @Transactional
    public void register(SocialProfile profile, String username, String role, String phoneNumber) {
        registerAccount(profile, username, role, phoneNumber, null);
    }

    @Transactional
    public void register(SocialProfile profile, String username, String role, String phoneNumber,
                         String organizationId) {
        registerAccount(profile, username, role, phoneNumber, organizationId);
    }

    private void registerAccount(SocialProfile profile, String username, String role,
                                 String phoneNumber, String organizationId) {
        profile.requireValid();
        username = normalizeUsername(username);
        if (role == null || !Set.of("TRACKER", "ADMIN").contains(role)
                || !username.matches("[a-zA-Z0-9._-]{6,100}")) {
            throw new IllegalArgumentException("Проверьте имя пользователя и роль.");
        }
        if (findIdentity(profile).isPresent() || users.existsByUsername(username)
                || users.existsByEmailIgnoreCase(profile.email())) {
            throw new IllegalArgumentException("Аккаунт уже существует. Воспользуйтесь привязкой к существующему аккаунту.");
        }
        var user = new UserEntity();
        user.setUsername(username);
        user.setEmail(profile.email());
        user.setFullName(profile.name());
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            if (!phoneNumber.matches("\\+?\\d{10,15}")) throw new IllegalArgumentException("Проверьте номер телефона.");
            user.setPhoneNumber(phoneNumber);
        }
        assignIdentity(user, profile);
        // A non-guessable password hash keeps existing password-based code compatible.
        user.setPasswordHash(passwords.encode(UUID.randomUUID().toString()));
        user.setActive(false);
        user.setAccountNonLocked(true);
        user.getRoles().add(roles.findByCode(role).orElseThrow(
                () -> new IllegalArgumentException("Роль недоступна.")));
        user.setOrganization(resolveOrganization(organizationId));
        users.saveAndFlush(user);
        events.publishEvent(new RegistrationRequestedEvent(user.getUsername(), user.getFullName(), role));
    }

    private OrganizationEntity resolveOrganization(String organizationId) {
        UUID id;
        try {
            id = organizationId == null || organizationId.isBlank()
                    ? OrganizationService.DEFAULT_ORGANIZATION_ID
                    : UUID.fromString(organizationId);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Выберите организацию из списка.");
        }
        return organizations.findById(id)
                .filter(organization -> organization.isAvailable(LocalDate.now()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Выбранная организация недоступна для регистрации."));
    }

    @Transactional
    public AuthorizedUser link(SocialProfile profile, String username, String password) {
        profile.requireValid();
        var user = users.findByUsername(username).orElseThrow(SocialAccountService::invalidLogin);
        if (user.getPasswordHash() == null || !passwords.matches(password, user.getPasswordHash())) {
            throw invalidLogin();
        }
        principal(user); // Respect activation and lock state before changing any identity.
        if (identity(user, profile) != null || findIdentity(profile).isPresent()) {
            throw new IllegalArgumentException("Аккаунт уже связан с этим провайдером.");
        }
        assignIdentity(user, profile);
        users.saveAndFlush(user);
        return principal(user);
    }

    private Optional<UserEntity> findIdentity(SocialProfile profile) {
        return GOOGLE.equals(profile.provider()) ? users.findByGoogleId(profile.id()) : users.findByYandexId(profile.id());
    }

    private String identity(UserEntity user, SocialProfile profile) {
        return GOOGLE.equals(profile.provider()) ? user.getGoogleId() : user.getYandexId();
    }

    private void assignIdentity(UserEntity user, SocialProfile profile) {
        if (GOOGLE.equals(profile.provider())) user.setGoogleId(profile.id());
        else user.setYandexId(profile.id());
    }

    private AuthorizedUser principal(UserEntity user) {
        if (!Boolean.TRUE.equals(user.getActive()) || !Boolean.TRUE.equals(user.getAccountNonLocked())) {
            throw new IllegalArgumentException("Аккаунт ожидает активации или заблокирован. Обратитесь к администратору.");
        }
        if (isOverlord(user)) {
            return AuthorizedUserMapper.map(user);
        }
        if (user.getOrganization() == null) {
            accessNotifier.notifyAccessDenied(user, null);
            throw new IllegalArgumentException(
                    "Пользователю не назначена организация. Обратитесь к владельцу платформы.");
        }
        if (!user.getOrganization().isAvailable(LocalDate.now())) {
            accessNotifier.notifyAccessDenied(user, user.getOrganization());
            throw new IllegalArgumentException(
                    "Организация заблокирована или срок её действия истёк. Обратитесь к владельцу платформы.");
        }
        return AuthorizedUserMapper.map(user);
    }

    private static boolean isOverlord(UserEntity user) {
        return user.getRoles().stream()
                .anyMatch(role -> CurrentUserService.OVERLORD.equals(role.getCode()));
    }

    public static String normalizeUsername(String username) {
        if (username == null) return "";
        String value = username.trim();
        return value.startsWith("@") ? value.substring(1) : value;
    }

    private static IllegalArgumentException invalidLogin() {
        return new IllegalArgumentException("Неверные данные аккаунта.");
    }
}

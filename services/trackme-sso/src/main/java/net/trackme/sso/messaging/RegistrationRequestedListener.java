package net.trackme.sso.messaging;

import static net.trackme.sso.dao.UserSpecification.byRole;

import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.sso.config.AppProperties;
import net.trackme.sso.dao.entity.UserEntity;
import net.trackme.sso.dao.repository.UserRepository;
import net.trackme.sso.services.EmailService;
import org.springframework.dao.DataAccessException;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationRequestedListener {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final AppProperties appProperties;
    private final UserRepository users;
    private final EmailService emailService;

    @TransactionalEventListener
    public void notifySuperAdmin(RegistrationRequestedEvent event) {
        var role = switch (event.role()) {
            case "TRACKER" -> "трекера";
            case "ADMIN" -> "администратора";
            default -> null;
        };
        if (role == null) {
            return;
        }

        var mail = appProperties.getMail();
        var section = "TRACKER".equals(event.role()) ? "list-trackers" : "list-admins";
        var link = UriComponentsBuilder.fromUriString(appProperties.getFrontendUrl())
                .pathSegment(section).build().toUriString();

        try {
            emailService.sendMail(mail.getRegistrationRecipient(), mail.getFrom(),
                    "[" + mail.getSubject() + "] Запрос на регистрацию",
                    "email-registration-request.html", Map.of(
                            "recipientName", recipientName(mail),
                            "fullName", StringUtils.hasText(event.fullName()) ? event.fullName() : "ФИО не указано",
                            "username", event.username(),
                            "role", role,
                            "approvalLink", link));
        } catch (MailException exception) {
            log.warn("Registration request email failed for role {}", event.role(), exception);
        }
    }

    private String recipientName(AppProperties.MailProperties mail) {
        if (StringUtils.hasText(mail.getRegistrationRecipientName())) {
            return mail.getRegistrationRecipientName();
        }
        try {
            return users.findByEmail(mail.getRegistrationRecipient())
                    .filter(user -> Boolean.TRUE.equals(user.getActive()))
                    .filter(user -> user.getRoles().stream().anyMatch(role -> SUPER_ADMIN.equals(role.getCode())))
                    .or(this::findOnlyActiveSuperAdmin)
                    .map(UserEntity::getFullName)
                    .filter(StringUtils::hasText)
                    .orElse("");
        } catch (DataAccessException exception) {
            log.warn("Could not look up the registration email recipient's name", exception);
            return "";
        }
    }

    private Optional<UserEntity> findOnlyActiveSuperAdmin() {
        var activeSuperAdmins = byRole(SUPER_ADMIN)
                .and((root, query, builder) -> builder.isTrue(root.get("active")));
        var matches = users.findBy(activeSuperAdmins, query -> query.limit(2).all());
        return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
    }
}

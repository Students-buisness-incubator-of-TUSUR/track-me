package net.trackme.sso.messaging;

import static net.trackme.sso.dao.UserSpecification.byRole;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
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
    private static final String ADMIN = "ADMIN";
    private static final String TRACKER = "TRACKER";

    private final AppProperties appProperties;
    private final UserRepository users;
    private final EmailService emailService;

    @TransactionalEventListener
    public void notifyApprovers(RegistrationRequestedEvent event) {
        var role = switch (event.role()) {
            case TRACKER -> "трекера";
            case ADMIN -> "администратора";
            default -> null;
        };
        if (role == null) {
            return;
        }

        var mail = appProperties.getMail();
        var isTracker = TRACKER.equals(event.role());
        var section = isTracker ? "list-trackers" : "list-admins";
        var link = UriComponentsBuilder.fromUriString(appProperties.getFrontendUrl())
                .pathSegment(section).build().toUriString();

        Map<String, Object> requestDetails = Map.of(
                "fullName", StringUtils.hasText(event.fullName()) ? event.fullName() : "ФИО не указано",
                "username", event.username(), "role", role, "approvalLink", link);
        var superAdminEmail = mail.getRegistrationRecipient();
        sendRegistrationEmail(superAdminEmail, recipientName(mail), requestDetails);
        if (isTracker) {
            notifyAdmins(superAdminEmail, requestDetails);
        }
    }

    private void notifyAdmins(String superAdminEmail, Map<String, Object> requestDetails) {
        var addresses = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        addresses.add(superAdminEmail);
        for (var admin : findActiveAdmins()) {
            var email = admin.getEmail().strip();
            if (!email.isEmpty() && addresses.add(email)) {
                sendRegistrationEmail(email, admin.getFullName(), requestDetails);
            }
        }
    }

    private List<UserEntity> findActiveAdmins() {
        var activeAdmins = byRole(ADMIN).and((root, query, builder) ->
                builder.and(builder.isTrue(root.get("active")), builder.isTrue(root.get("accountNonLocked"))));
        try {
            return users.findAll(activeAdmins);
        } catch (DataAccessException exception) {
            log.warn("Could not look up administrators for tracker registration email", exception);
            return List.of();
        }
    }

    private void sendRegistrationEmail(String email, String name, Map<String, Object> requestDetails) {
        var mail = appProperties.getMail();
        var variables = new HashMap<>(requestDetails);
        variables.put("recipientName", StringUtils.hasText(name) ? name : "");
        try {
            emailService.sendMail(email, mail.getFrom(),
                    "[" + mail.getSubject() + "] Запрос на регистрацию",
                    "email-registration-request.html", variables);
        } catch (MailException exception) {
            log.warn("Registration request email failed", exception);
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

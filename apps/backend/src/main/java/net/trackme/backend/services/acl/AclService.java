package net.trackme.backend.services.acl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AclService {
    private static final List<Permission> FULL_PERMISSIONS = List.of(
            BasePermission.READ,
            BasePermission.WRITE,
            BasePermission.DELETE,
            BasePermission.ADMINISTRATION
    );
    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final MutableAclService mutableAclService;

    public void createAclForUser(Object identity, String ownerUsername) {
        log.info("Создание ACL для пользователя '{}' и объекта '{}'", ownerUsername, identity);
        createAcl(identity, ownerUsername, ownerUsername, null);
    }

    public void createAclForUser(Object identity, String ownerUsername, String creatorUsername) {
        log.info("Создание ACL для пользователя '{}' (создатель: '{}') и объекта '{}'", ownerUsername, creatorUsername, identity);
        createAcl(identity, ownerUsername, creatorUsername, null);
    }

    public void createAclForUserWithParent(Object identity, String ownerUsername, String creatorUsername, Object parent) {
        log.info("Создание ACL для пользователя '{}' (создатель: '{}'), объекта '{}' с родителем '{}'", ownerUsername, creatorUsername, identity, parent);
        createAcl(identity, ownerUsername, creatorUsername, parent);
    }

    public void addPermissionsToUser(MutableAcl acl, String username, List<? extends Permission> permissions) {
        log.debug("Добавление разрешений {} пользователю '{}' для ACL '{}'", permissions, username, acl.getObjectIdentity());
        addPermissions(acl, new PrincipalSid(username), permissions);
    }

    public void addPermissionsToRole(MutableAcl acl, String role, List<? extends Permission> permissions) {
        log.debug("Добавление разрешений {} роли '{}' для ACL '{}'", permissions, role, acl.getObjectIdentity());
        addPermissions(acl, new GrantedAuthoritySid(role), permissions);
    }

    public void deleteAcl(Object identity) {
        log.info("Удаление ACL для объекта '{}'", identity);
        ObjectIdentity objectIdentity = new ObjectIdentityImpl(identity);
        mutableAclService.deleteAcl(objectIdentity, true);
        log.info("ACL для объекта '{}' успешно удалён.", identity);
    }

    /**
     * Обновить владельца ACL по объектному идентификатору.
     *
     * @param identity         сущность, для которой меняется владелец
     * @param newOwnerUsername имя нового владельца
     */
    public void updateAclOwner(Object identity, String newOwnerUsername) {
        log.info("Обновление владельца ACL для объекта '{}' на '{}'", identity, newOwnerUsername);
        ObjectIdentity oid = new ObjectIdentityImpl(identity);
        MutableAcl acl = (MutableAcl) mutableAclService.readAclById(oid);
        updateAclOwner(acl, newOwnerUsername);
    }

    /**
     * Обновить владельца у заданного MutableAcl.
     * Старые ACE для предыдущего владельца удаляются,
     * новому владельцу назначаются полные права.
     *
     * @param acl              ACL, для которого меняется владелец
     * @param newOwnerUsername имя нового владельца
     */
    public void updateAclOwner(MutableAcl acl, String newOwnerUsername) {
        Sid oldOwnerSid = acl.getOwner();
        PrincipalSid newOwnerSid = new PrincipalSid(newOwnerUsername);

        log.debug("Смена владельца ACL '{}' c '{}' на '{}'",
                acl.getObjectIdentity(), oldOwnerSid, newOwnerUsername);

        // Установить нового владельца
        acl.setOwner(newOwnerSid);

        // Удалить все ACE, принадлежавшие старому владельцу
        for (int i = acl.getEntries().size() - 1; i >= 0; i--) {
            if (acl.getEntries().get(i).getSid().equals(oldOwnerSid)) {
                acl.deleteAce(i);
            }
        }

        // Назначить новому владельцу полный набор прав
        addPermissionsToUser(acl, newOwnerUsername, FULL_PERMISSIONS);

        // Сохранить изменения
        mutableAclService.updateAcl(acl);
        log.info("Владелец ACL '{}' успешно обновлён на '{}'",
                acl.getObjectIdentity(), newOwnerUsername);
    }

    /*
     * Внутренние методы
     */
    private void createAcl(Object identity, String ownerUsername, String creatorUsername, Object parent) {
        log.debug("Внутреннее создание ACL для '{}', owner='{}', creator='{}', parent='{}'", identity, ownerUsername, creatorUsername, parent);
        ObjectIdentity objectIdentity = new ObjectIdentityImpl(identity);
        MutableAcl acl = mutableAclService.createAcl(objectIdentity);

        if (parent != null) {
            log.debug("Установка родителя для ACL '{}' на '{}'", identity, parent);
            ObjectIdentity parentIdentity = new ObjectIdentityImpl(parent);
            acl.setParent(mutableAclService.readAclById(parentIdentity));
        }

        acl.setOwner(new PrincipalSid(ownerUsername));
        addPermissionsToUser(acl, ownerUsername, FULL_PERMISSIONS);

        if (!ownerUsername.equals(creatorUsername)) {
            addPermissionsToUser(acl, creatorUsername, FULL_PERMISSIONS);
        }

        grantFullAccessToDefaultRoles(acl);

        mutableAclService.updateAcl(acl);
        log.info("ACL для '{}' успешно создан/обновлён", identity);
    }

    private void grantFullAccessToDefaultRoles(MutableAcl acl) {
        log.debug("Назначение полных разрешений по-умолчанию ролям '{}' и '{}'", ROLE_SUPER_ADMIN, ROLE_ADMIN);
        addPermissionsToRole(acl, ROLE_SUPER_ADMIN, FULL_PERMISSIONS);
        addPermissionsToRole(acl, ROLE_ADMIN, FULL_PERMISSIONS);
    }

    private void addPermissions(MutableAcl acl, Sid sid, List<? extends Permission> permissions) {
        for (Permission permission : permissions) {
            log.trace("Вставка разрешения '{}' для SID '{}' в ACL '{}'", permission, sid, acl.getObjectIdentity());
            acl.insertAce(acl.getEntries().size(), permission, sid, true);
        }
        mutableAclService.updateAcl(acl);
        log.debug("ACL '{}' обновлён с новыми разрешениями для SID '{}'", acl.getObjectIdentity(), sid);
    }

    public void createAclForUserWithParent(Object identity, String username, Object parent) {
        log.info("Создание ACL для пользователя '{}' с родителем '{}'", username, parent);
        createAclForUserWithParent(identity, username, username, parent);
    }
}
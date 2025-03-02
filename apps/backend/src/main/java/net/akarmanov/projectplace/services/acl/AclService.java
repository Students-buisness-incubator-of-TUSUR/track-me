package net.akarmanov.projectplace.services.acl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.akarmanov.projectplace.domain.TeamCard;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.Permission;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AclService {
  private final MutableAclService mutableAclService;

  public MutableAcl createAcl(Object identity) {
    return createAcl(identity, List.of(
        BasePermission.READ,
        BasePermission.WRITE,
        BasePermission.DELETE));
  }

  public MutableAcl createAcl(Object identity,
                              List<? extends Permission> permissions) {
    var objectIdentity = new ObjectIdentityImpl(identity);
    var acl = mutableAclService.createAcl(objectIdentity);
    return addPermissions(acl, permissions);
  }

  public MutableAcl addPermissions(MutableAcl acl,
                                   List<? extends Permission> permissions) {
    var owner = acl.getOwner();
    for (var permission : permissions) {
      acl.insertAce(acl.getEntries().size(), permission, owner, true);
      log.debug("Добавлено разрешение {} для SID {}", permission, owner);
    }
    return mutableAclService.updateAcl(acl);
  }

  public void createAclWithParent(Object identity, Object parent) {
    var parentIdentity = new ObjectIdentityImpl(parent);
    log.debug("Создание ACL для {} с родителем {}", identity, parent);
    var acl = createAcl(identity);
    acl.setParent(mutableAclService.readAclById(parentIdentity));
    acl.setEntriesInheriting(true);
    mutableAclService.updateAcl(acl);
  }

  public void deleteAcl(TeamCard teamCard) {
    var objectIdentity = new ObjectIdentityImpl(teamCard);
    mutableAclService.deleteAcl(objectIdentity, true);
    log.debug("Удален ACL для {}", objectIdentity);
  }

  public void updateAcl(Object identity, String principle) {
    log.debug("Обновление ACL для {} с SID {}", identity, principle);
    var objectIdentity = new ObjectIdentityImpl(identity);
    var sid = new PrincipalSid(principle);
    MutableAcl acl;
    try {
      acl = (MutableAcl) mutableAclService.readAclById(objectIdentity);
    } catch (NotFoundException e) {
      log.debug("ACL не найден, создается новый");
      acl = mutableAclService.createAcl(objectIdentity);
    }
    acl.setOwner(sid);
    mutableAclService.updateAcl(acl);
    log.debug("Обновлен ACL для {} с SID {}", identity, principle);
  }
}

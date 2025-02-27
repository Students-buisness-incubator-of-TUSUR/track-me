package net.akarmanov.projectplace.services.acl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.domain.TeamCard;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AclService {
  private final MutableAclService mutableAclService;

  public MutableAcl createAcl(Object identity) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return createAcl(identity, authentication.getName(), List.of(
        BasePermission.READ,
        BasePermission.WRITE,
        BasePermission.DELETE));
  }

  public void createAcl(Object identity, String principle) {
    createAcl(identity, principle, List.of(
        BasePermission.READ,
        BasePermission.WRITE,
        BasePermission.DELETE));
  }

  public MutableAcl createAcl(Object identity, String principle, List<? extends Permission> permissions) {
    var objectIdentity = new ObjectIdentityImpl(identity);
    var sid = new PrincipalSid(principle);
    var acl = mutableAclService.createAcl(objectIdentity);
    return addPermissions(acl, sid, permissions);
  }

  public void updateAcl(Object identity, String principle) {
    var objectIdentity = new ObjectIdentityImpl(identity);
    var sid = new PrincipalSid(principle);
    var acl = (MutableAcl) mutableAclService.readAclById(objectIdentity);
    acl.setOwner(sid);
    mutableAclService.updateAcl(acl);
  }

  public MutableAcl addPermissions(MutableAcl acl, Sid sid, List<? extends Permission> permissions) {
    acl.setOwner(sid);
    for (var permission : permissions) {
      acl.insertAce(acl.getEntries().size(), permission, sid, true);
    }
    return mutableAclService.updateAcl(acl);
  }

  public void createAclWithParent(Object identity, Object parent) {
    var parentObjectIdentity = new ObjectIdentityImpl(parent);
    var acl = createAcl(identity);
    var parentAcl = (MutableAcl) mutableAclService.readAclById(parentObjectIdentity);
    acl.setParent(parentAcl);
    mutableAclService.updateAcl(acl);
  }

  public void removePermission(ObjectIdentity objectIdentity, Sid sid, Permission permission) {
    // Remove permission from the ACL for the given object identity and SID
    MutableAcl acl = (MutableAcl) mutableAclService.readAclById(objectIdentity);
    List<AccessControlEntry> entries = acl.getEntries();
    for (int i = 0; i < entries.size(); i++) {
      AccessControlEntry entry = entries.get(i);
      if (entry.getSid().equals(sid) && entry.getPermission().equals(permission)) {
        acl.deleteAce(i);
        break;
      }
    }
    mutableAclService.updateAcl(acl);
  }

  public void deleteAcl(TeamCard teamCard) {
    var objectIdentity = new ObjectIdentityImpl(teamCard);
    mutableAclService.deleteAcl(objectIdentity, true);
  }
}

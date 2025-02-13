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
  private final MutableAclService aclService;

  public MutableAcl createAcl(Object identity) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return createAcl(identity, authentication.getName(), List.of(
        BasePermission.READ,
        BasePermission.WRITE,
        BasePermission.DELETE));
  }

  public MutableAcl createAcl(Object identity, String principle) {
    return createAcl(identity, principle, List.of(
        BasePermission.READ,
        BasePermission.WRITE,
        BasePermission.DELETE));
  }

  public MutableAcl createAcl(Object identity, String principle, List<? extends Permission> permissions) {
    var objectIdentity = new ObjectIdentityImpl(identity);
    var sid = new PrincipalSid(principle);
    aclService.createAcl(objectIdentity);
    return addPermissions(objectIdentity, sid, permissions);
  }

  public void updateAcl(Object identity, String principle) {
    var objectIdentity = new ObjectIdentityImpl(identity);
    var sid = new PrincipalSid(principle);
    var acl = (MutableAcl) aclService.readAclById(objectIdentity);
    acl.setOwner(sid);
    aclService.updateAcl(acl);
  }

  public MutableAcl addPermissions(ObjectIdentity objectIdentity, Sid sid, List<? extends Permission> permissions) {
    // Add permission to the ACL for the given object identity and SID
    MutableAcl acl = (MutableAcl) aclService.readAclById(objectIdentity);
    acl.setOwner(sid);
    for (var permission : permissions) {
      acl.insertAce(acl.getEntries().size(), permission, sid, true);
    }
    return aclService.updateAcl(acl);
  }

  public void createAclWithParent(Object identity, Object parent) {
    var parentObjectIdentity = new ObjectIdentityImpl(parent);
    var acl = createAcl(identity);
    var parentAcl = (MutableAcl) aclService.readAclById(parentObjectIdentity);
    acl.setParent(parentAcl);
    aclService.updateAcl(acl);
  }

  public void removePermission(ObjectIdentity objectIdentity, Sid sid, Permission permission) {
    // Remove permission from the ACL for the given object identity and SID
    MutableAcl acl = (MutableAcl) aclService.readAclById(objectIdentity);
    List<AccessControlEntry> entries = acl.getEntries();
    for (int i = 0; i < entries.size(); i++) {
      AccessControlEntry entry = entries.get(i);
      if (entry.getSid().equals(sid) && entry.getPermission().equals(permission)) {
        acl.deleteAce(i);
        break;
      }
    }
    aclService.updateAcl(acl);
  }

  public void deleteAcl(TeamCard teamCard) {
    var objectIdentity = new ObjectIdentityImpl(teamCard);
    aclService.deleteAcl(objectIdentity, true);
  }
}

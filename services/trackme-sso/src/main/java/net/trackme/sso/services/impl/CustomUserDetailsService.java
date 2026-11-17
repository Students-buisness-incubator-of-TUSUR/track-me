package net.trackme.sso.services.impl;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.trackme.sso.dao.entity.UserEntity;
import net.trackme.sso.dao.repository.UserRepository;
import net.trackme.sso.mapper.AuthorizedUserMapper;
import net.trackme.sso.services.CurrentUserService;
import net.trackme.sso.services.OrganizationAccessNotifier;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Загружает пользователя и проверяет доступность его организации.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private static final String ORGANIZATION_NOT_ASSIGNED =
      "Пользователю не назначена организация. Обратитесь к владельцу платформы.";
  private static final String ORGANIZATION_UNAVAILABLE =
      "Организация заблокирована или срок её действия истёк. Обратитесь к владельцу платформы.";

  private final UserRepository userRepository;

  private final OrganizationAccessNotifier organizationAccessNotifier;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    var userEntity = userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    checkOrganizationAccess(userEntity);
    return AuthorizedUserMapper.map(userEntity);
  }

  private void checkOrganizationAccess(UserEntity userEntity) {
    if (isOverlord(userEntity)) {
      return;
    }
    var organization = userEntity.getOrganization();
    if (organization == null) {
      organizationAccessNotifier.notifyAccessDenied(userEntity, null);
      throw new LockedException(ORGANIZATION_NOT_ASSIGNED);
    }
    if (!organization.isAvailable(LocalDate.now())) {
      organizationAccessNotifier.notifyAccessDenied(userEntity, organization);
      throw new LockedException(ORGANIZATION_UNAVAILABLE);
    }
  }

  private boolean isOverlord(UserEntity userEntity) {
    Set<String> roles = userEntity.getRoles().stream()
        .map(role -> role.getCode())
        .collect(Collectors.toSet());
    return roles.contains(CurrentUserService.OVERLORD);
  }
}

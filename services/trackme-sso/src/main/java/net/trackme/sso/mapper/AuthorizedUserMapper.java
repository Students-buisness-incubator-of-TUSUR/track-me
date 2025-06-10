package net.trackme.sso.mapper;

import lombok.experimental.UtilityClass;
import net.trackme.sso.dao.entity.RoleEntity;
import net.trackme.sso.dao.entity.UserEntity;
import net.trackme.sso.dto.AuthorizedUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class AuthorizedUserMapper {
  public AuthorizedUser map(UserEntity entity) {
    List<GrantedAuthority> authorities = getUserAuthorities(entity);
    return AuthorizedUser.builder(
            entity.getUsername(),
            entity.getPasswordHash(),
            entity.getActive(),
            true,
            true,
            true,
            authorities)
        .id(entity.getId())
        .fullName(entity.getFullName())
        .avatarUrl(entity.getAvatarUrl())
        .email(entity.getEmail())
        .phoneNumber(entity.getPhoneNumber())
        .accountNonLocked(entity.getAccountNonLocked())
        .build();
  }

  public List<GrantedAuthority> getUserAuthorities(UserEntity entity) {
    return entity.getRoles().stream()
        .filter(RoleEntity::getActive)
        .map(authority -> new SimpleGrantedAuthority(authority.getCode()))
        .collect(Collectors.toList());
  }
}

package net.akarmanov.projectplace.sso.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
public class AuthorizedUser extends User implements OAuth2User {
  private UUID id;

  private String fullName;

  private String email;

  private String avatarUrl;

  private Map<String, Object> oauthAttributes;

  public AuthorizedUser(String username,
                        String password,
                        Collection<? extends GrantedAuthority> authorities) {
    super(username, password, authorities);
  }

  public AuthorizedUser(
      String username,
      String password,
      boolean enabled,
      boolean accountNonExpired,
      boolean credentialsNonExpired,
      boolean accountNonLocked,
      Collection<? extends GrantedAuthority> authorities
  ) {
    super(username,
        password,
        enabled,
        accountNonExpired,
        credentialsNonExpired,
        accountNonLocked,
        authorities);
  }

  public static AuthorizedUserBuilder builder(String username,
                                              String password,
                                              Collection<? extends GrantedAuthority> authorities) {
    return new AuthorizedUserBuilder(username, password, authorities);
  }

  public static AuthorizedUserBuilder builder(
      String username,
      String password,
      boolean enabled,
      boolean accountNonExpired,
      boolean credentialsNonExpired,
      boolean accountNonLocked,
      Collection<? extends GrantedAuthority> authorities
  ) {
    return new AuthorizedUserBuilder(username,
        password,
        enabled,
        accountNonExpired,
        credentialsNonExpired,
        accountNonLocked,
        authorities);
  }

  public String getEmail() {
    return this.getUsername();
  }

  @Override
  public Map<String, Object> getAttributes() {
    return oauthAttributes;
  }

  @Override
  public String getName() {
    return this.getUsername();
  }

  public static class AuthorizedUserBuilder {

    private final AuthorizedUser entity;

    AuthorizedUserBuilder(String username,
                          String password,
                          Collection<? extends GrantedAuthority> authorities) {
      if (password == null) {
        password = "";
      }
      this.entity = new AuthorizedUser(username, password, authorities);
    }

    AuthorizedUserBuilder(
        String username,
        String password,
        boolean enabled,
        boolean accountNonExpired,
        boolean credentialsNonExpired,
        boolean accountNonLocked,
        Collection<? extends GrantedAuthority> authorities
    ) {
      this.entity = new AuthorizedUser(username,
          password,
          enabled,
          accountNonExpired,
          credentialsNonExpired,
          accountNonLocked,
          authorities);
    }

    public AuthorizedUserBuilder id(UUID id) {
      this.entity.setId(id);
      return this;
    }

    public AuthorizedUserBuilder fullName(String fullName) {
      this.entity.setFullName(fullName);
      return this;
    }

    public AuthorizedUserBuilder avatarUrl(String avatarUrl) {
      this.entity.setAvatarUrl(avatarUrl);
      return this;
    }

    public AuthorizedUserBuilder email(String email) {
      this.entity.setEmail(email);
      return this;
    }

    public AuthorizedUserBuilder oauthAttributes(Map<String, Object> userSasInfo) {
      this.entity.setOauthAttributes(userSasInfo);
      return this;
    }

    public AuthorizedUser build() {
      return this.entity;
    }
  }
}

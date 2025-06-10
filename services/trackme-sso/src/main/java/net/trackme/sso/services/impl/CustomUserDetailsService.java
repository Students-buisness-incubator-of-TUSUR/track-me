package net.trackme.sso.services.impl;

import lombok.RequiredArgsConstructor;
import net.trackme.sso.dao.repository.UserRepository;
import net.trackme.sso.mapper.AuthorizedUserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    var userEntity = userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    return AuthorizedUserMapper.map(userEntity);
  }
}

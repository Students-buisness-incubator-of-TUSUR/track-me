package net.trackme.sso.services;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.sso.dto.UserDto;
import net.trackme.sso.dto.UserUpdateDto;
import net.trackme.sso.mapper.UserMapper;
import net.trackme.sso.messaging.UserUpdatedInternalEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultAccountService implements AccountService {

  private final ApplicationEventPublisher applicationEventPublisher;

  private final UserService userService;

  private final UserMapper userMapper;

  @Override
  public UserDto getUser(String username) {
    var userEntity = userService.findByUsername(username);
    return userMapper.userEntityToUserDto(userEntity);
  }

  @Override
  @Transactional
  public void updateUser(@Valid UserUpdateDto userDto, Authentication authentication) {
    var username = authentication.getName();
    var userEntity = userService.findByUsername(username);

    userMapper.updateUserEntityFromUserDto(userDto, userEntity);
    userService.save(userEntity);

    log.info("[Sync] Отправка синхронизирующего ивента. ");

    applicationEventPublisher.publishEvent(new UserUpdatedInternalEvent(
        username,
        userDto.fullName())
    );

    log.info("[Sync] Ивент отправлен!. ");
  }

  @Override
  public void changePassword(String newPassword,
                             String oldPassword, Authentication authentication) {
    var username = authentication.getName();
    userService.changePassword(username, newPassword, oldPassword);
  }
}

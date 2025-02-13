package net.akarmanov.projectplace.services.auth;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.domain.User;
import net.akarmanov.projectplace.models.UserRole;
import net.akarmanov.projectplace.rest.api.auth.JwtAuthenticationResponse;
import net.akarmanov.projectplace.rest.api.dto.SingInRequest;
import net.akarmanov.projectplace.rest.api.dto.SingUpRequest;
import net.akarmanov.projectplace.services.jwt.JwtService;
import net.akarmanov.projectplace.services.stream.StreamService;
import net.akarmanov.projectplace.services.user.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class AuthenticationServiceImpl implements AuthenticationService {
  private final UserService userService;

  private final JwtService jwtService;

  private final PasswordEncoder passwordEncoder;

  private final AuthenticationManager authenticationManager;

  private final StreamService streamService;

  @Override
  @Transactional
  public JwtAuthenticationResponse singUp(SingUpRequest singUpRequest) {
    var stream = streamService.getCurrentStream();
    var user = User.builder()
        .firstName(singUpRequest.getFirstName())
        .lastName(singUpRequest.getLastName())
        .middleName(singUpRequest.getMiddleName())
        .telegramId(singUpRequest.getTelegramId())
        .phoneNumber(singUpRequest.getPhoneNumber())
        .role(UserRole.valueOf(singUpRequest.getRole().toString()))
        .password(passwordEncoder.encode(singUpRequest.getPassword()))
        .email(singUpRequest.getEmail())
        .build();

    stream.addUser(userService.createUser(user));
    streamService.save(stream);
    var token = jwtService.generateToken(user);
    return new JwtAuthenticationResponse(token);
  }

  @Override
  public JwtAuthenticationResponse singIn(SingInRequest request) {
    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
        request.getTelegramId(),
        request.getPassword())
    );
    var user = userService.loadUserByUsername(request.getTelegramId());

    var token = jwtService.generateToken(user);
    return new JwtAuthenticationResponse(token);
  }
}

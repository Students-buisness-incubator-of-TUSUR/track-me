package net.akarmanov.projectplace.authservice.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/users/")
public interface UserRestController {
  @PutMapping("/update")
  ResponseEntity<Void> updateUser(@RequestBody UserUpdateDto user);
}

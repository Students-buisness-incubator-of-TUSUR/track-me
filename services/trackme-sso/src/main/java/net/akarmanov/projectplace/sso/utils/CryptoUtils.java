package net.akarmanov.projectplace.sso.utils;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.springframework.security.crypto.codec.Hex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@UtilityClass
public class CryptoUtils {

  /**
   * Получить hash указанной строки.
   */
  @SneakyThrows
  public String hash(String input) {
    var md = MessageDigest.getInstance("SHA3-256");
    byte[] result = md.digest(input.getBytes(StandardCharsets.UTF_8));
    return new String(Hex.encode(result));
  }
}

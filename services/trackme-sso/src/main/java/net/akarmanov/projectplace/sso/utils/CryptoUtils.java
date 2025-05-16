package net.akarmanov.projectplace.sso.utils;

import lombok.experimental.UtilityClass;
import net.akarmanov.projectplace.sso.exception.CryptoException;
import org.springframework.security.crypto.codec.Hex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@UtilityClass
public class CryptoUtils {

  /**
   * Получить hash указанной строки.
   */
  public String hash(String input) {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("SHA3-256");
    } catch (NoSuchAlgorithmException ex) {
      throw new CryptoException(ex.getMessage(), ex);
    }
    byte[] result = md.digest(input.getBytes(StandardCharsets.UTF_8));
    return new String(Hex.encode(result));
  }
}

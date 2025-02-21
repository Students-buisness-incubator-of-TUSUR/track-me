package net.akarmanov.projectplace.services.nti;

import net.akarmanov.projectplace.services.exceptions.PPNotFoundException;

import java.util.UUID;

public class NtiMarketNotFoundException extends PPNotFoundException {
  public NtiMarketNotFoundException(UUID ntiMarketId) {
    super("Не найден рынок NTI с идентификатором " + ntiMarketId);
  }
}

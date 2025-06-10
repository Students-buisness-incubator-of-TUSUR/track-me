package net.trackme.backend.services.nti;

import net.trackme.backend.services.exceptions.PPNotFoundException;

import java.util.UUID;

public class NtiMarketNotFoundException extends PPNotFoundException {
  public NtiMarketNotFoundException(UUID ntiMarketId) {
    super("Не найден рынок NTI с идентификатором " + ntiMarketId);
  }
}

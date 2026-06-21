package net.trackme.backend.services.nti;

import net.trackme.backend.services.exceptions.PPNotFoundException;

import java.util.List;
import java.util.UUID;

public class NtiMarketNotFoundException extends PPNotFoundException {

  public NtiMarketNotFoundException(List<UUID> ids) {
    super("Не найдены рынки NTI с идентификаторами " + ids);
  }
}

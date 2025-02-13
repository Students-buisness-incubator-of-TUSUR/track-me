package net.akarmanov.projectplace.services.exceptions;

import java.util.UUID;

public class MeetingNotFoundException extends PPNotFoundException {
  public MeetingNotFoundException(UUID meetingId, UUID teamCardId) {
    super("Встреча с id " + meetingId + " не найдена для карточки с id " + teamCardId);
  }

  public MeetingNotFoundException(UUID meetingId) {
    super("Встреча с id " + meetingId + " не найдена");
  }
}

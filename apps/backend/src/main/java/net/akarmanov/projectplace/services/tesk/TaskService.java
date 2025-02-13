package net.akarmanov.projectplace.services.tesk;

import net.akarmanov.projectplace.domain.Task;

import java.util.UUID;

public interface TaskService {
  Task addTask(UUID meetingId, Task createTask);

  Task updateTask(Task taskDto);

  void deleteTask(UUID taskId);

  void copyTask(UUID taskId, UUID meetingId);

  Task getTaskById(UUID taskId);
}

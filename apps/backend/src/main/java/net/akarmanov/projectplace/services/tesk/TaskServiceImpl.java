package net.akarmanov.projectplace.services.tesk;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.akarmanov.projectplace.domain.Task;
import net.akarmanov.projectplace.models.TaskStatus;
import net.akarmanov.projectplace.repos.TaskRepository;
import net.akarmanov.projectplace.services.acl.AclService;
import net.akarmanov.projectplace.services.exceptions.TaskNotFoundException;
import net.akarmanov.projectplace.services.meeting.MeetingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

  private final TaskRepository taskRepository;

  private final MeetingService meetingService;

  private final AclService aclService;

  @Override
  @Transactional
  @PreAuthorize("hasPermission(#meetingId, 'net.akarmanov.projectplace.domain.Meeting', 'READ')")
  public Task addTask(UUID meetingId, Task createTask) {
    var meeting = meetingService.getById(meetingId);
    createTask.setMeeting(meeting);
    createTask.setNumber(TrackNumberUtil.generateTrackNumber());
    createTask.setStatus(TaskStatus.NEW);
    var save = taskRepository.save(createTask);
    log.info("Task {} created", save.getId());
    aclService.createAclWithParent(save, meeting);
    return save;
  }

  @Override
  @Transactional
  @PreAuthorize("hasPermission(#updateTask, 'WRITE')")
  public Task updateTask(Task updateTask) {
    var oldTask = getTaskById(updateTask.getId());
    updateTask(updateTask, oldTask);
    var save = taskRepository.save(updateTask);
    log.info("Task {} updated", save.getId());
    return save;
  }

  private void updateTask(Task taskDto, Task task) {
    if (taskDto.getDescription() != null) {
      task.setDescription(taskDto.getDescription());
    }
    if (taskDto.getLink() != null) {
      task.setLink(taskDto.getLink());
    }
    if (taskDto.getStatus() != null) {
      task.setStatus(taskDto.getStatus());
    }
  }

  @Override
  @Transactional
  @PreAuthorize("hasPermission(#taskId, 'net.akarmanov.projectplace.domain.Task', 'DELETE')")
  public void deleteTask(UUID taskId) {
    var task = getTaskById(taskId);
    taskRepository.delete(task);
    log.info("Task {} deleted", taskId);
  }

  @Override
  @Transactional
  @PreAuthorize("hasPermission(#meetingId, 'net.akarmanov.projectplace.domain.Meeting', 'READ')")
  public void copyTask(UUID taskId, UUID meetingId) {
    var task = getTaskById(taskId);
    var meeting = meetingService.getById(meetingId);
    var copy = Task.copy(task);
    copy.setMeeting(meeting);
    var save = taskRepository.save(copy);
    log.info("Task {} copied to meeting {}", taskId, meetingId);
    aclService.createAclWithParent(save, meeting);
  }

  @Override
  public Task getTaskById(UUID taskId) {
    return taskRepository.findById(taskId).orElseThrow(
        () -> new TaskNotFoundException(taskId)
    );
  }
}

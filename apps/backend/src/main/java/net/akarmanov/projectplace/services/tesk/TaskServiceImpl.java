package net.akarmanov.projectplace.services.tesk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.akarmanov.projectplace.domain.Task;
import net.akarmanov.projectplace.mapping.TaskMapper;
import net.akarmanov.projectplace.repos.TaskRepository;
import net.akarmanov.projectplace.rest.api.dto.TaskCreateDto;
import net.akarmanov.projectplace.rest.api.dto.TaskDto;
import net.akarmanov.projectplace.rest.api.dto.TaskUpdateDto;
import net.akarmanov.projectplace.services.exceptions.TaskNotFoundException;
import net.akarmanov.projectplace.services.meeting.MeetingService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;

    private final TaskRepository taskRepository;

    private final MeetingService meetingService;

    @Override
    public TaskDto addTask(UUID meetingId, TaskCreateDto taskDto) {
        var meeting = meetingService.getById(meetingId);
        var task = Task.builder()
                .meeting(meeting)
                .description(taskDto.description())
                .status("")
                .number(TrackNumberUtil.generateTrackNumber())
                .link(taskDto.link())
                .build();
        var save = taskRepository.save(task);
        log.info("Task {} created", save.getId());
        return taskMapper.toDto(save);
    }

    @Override
    public TaskDto updateTask(TaskUpdateDto taskDto) {
        var task = getTaskById(taskDto.id());
        updateTask(taskDto, task);
        var save = taskRepository.save(task);
        log.info("Task {} updated", save.getId());
        return taskMapper.toDto(save);
    }

    private void updateTask(TaskUpdateDto taskDto, Task task) {
        if (taskDto.description() != null) {
            task.setDescription(taskDto.description());
        }
        if (taskDto.link() != null) {
            task.setLink(taskDto.link());
        }
        if (taskDto.status() != null) {
            task.setStatus(taskDto.status());
        }
    }

    @Override
    public void deleteTask(UUID taskId) {
        var task = getTaskById(taskId);
        taskRepository.delete(task);
        log.info("Task {} deleted", taskId);
    }

    @Override
    public void copyTask(UUID taskId, UUID meetingId) {
        var task = getTaskById(taskId);
        var meeting = meetingService.getById(meetingId);
        var copy = Task.copy(task);
        copy.setMeeting(meeting);
        taskRepository.save(copy);
        log.info("Task {} copied to meeting {}", taskId, meetingId);
    }

    @Override
    public Task getTaskById(UUID taskId) {
        return taskRepository.findById(taskId).orElseThrow(
                () -> new TaskNotFoundException(taskId)
        );
    }
}

package net.akarmanov.projectplace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.akarmanov.projectplace.models.TaskStatus;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "task")
public class Task {

  @Id
  @Column(nullable = false,
          updatable = false)
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(nullable = false)
  private String link;

  @Column(nullable = false)
  private Integer number;

  @Column(columnDefinition = "text")
  private String description;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private TaskStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "meeting_id",
              nullable = false)
  private Meeting meeting;

  public static Task copy(Task task) {
    return Task.builder()
        .link(task.getLink())
        .number(task.getNumber())
        .description(task.getDescription())
        .status(task.getStatus())
        .build();
  }

}
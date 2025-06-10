package net.trackme.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import net.trackme.backend.models.MeetingStatus;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@Table(name = "meeting")
@AllArgsConstructor
@NoArgsConstructor
public class Meeting {
  @Id
  @Column(nullable = false,
          updatable = false)
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column
  private String link;

  @Column(length = 32)
  private String number;

  @Column(nullable = false)
  private OffsetDateTime startDate;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Column
  private byte[] screenshot;

  @Enumerated(EnumType.STRING)
  @Column(length = 32)
  private MeetingStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id",
              nullable = false)
  private TeamCard teamCard;

  @Column(name = "tasks_current", length = 2048)
  private String tasksCurrentMeeting;

  @Column(name = "tasks_next", length = 2048)
  private String tasksNextMeeting;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "image")
  private byte[] imageBytes;

}
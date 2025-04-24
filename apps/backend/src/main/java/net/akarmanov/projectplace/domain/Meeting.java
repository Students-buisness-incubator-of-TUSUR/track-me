package net.akarmanov.projectplace.domain;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.akarmanov.projectplace.models.MeetingStatus;
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
  @Column(nullable = false,
          length = 32)
  private MeetingStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id",
              nullable = false)
  private TeamCard teamCard;

  @Column(name = "tasks_current", length = 2048)
  private String tasksCurrentMeeting;

  @Column(name = "tasks_next", length = 2048)
  private String tasksNextMeeting;

}
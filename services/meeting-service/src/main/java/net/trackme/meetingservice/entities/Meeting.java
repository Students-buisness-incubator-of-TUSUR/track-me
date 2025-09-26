package net.trackme.meetingservice.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

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
    @Column(
            nullable = false,
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

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private TeamStatus teamStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private MeetingStatus status;

    @Column(name = "team_card_id", nullable = false)
    private UUID teamCardId;

    @Column(name = "tasks_current", length = 2048)
    private String tasksCurrentMeeting;

    @Column(name = "tasks_next", length = 2048)
    private String tasksNextMeeting;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "image")
    private byte[] imageBytes;

}
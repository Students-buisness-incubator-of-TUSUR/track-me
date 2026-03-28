package net.trackme.meetingservice.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
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
    private String recordLink;

    @Column(length = 32)
    private String number;

    @Column(nullable = false)
    private OffsetDateTime startDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private TeamStatus teamStatus;

    @Column(name = "team_status_value", precision = 3, scale = 2)
    private java.math.BigDecimal teamStatusValue;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private MeetingStatus status;

    @Column(name = "tasks_current", length = 2048)
    private String tasksCurrentMeeting;

    @Column(name = "tasks_next", length = 2048)
    private String tasksNextMeeting;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "image")
    private byte[] imageBytes;

    // FK

    @Column(name = "team_card_id", nullable = true)
    private UUID teamCardId;

    @Column(name = "team_name")
    private String teamName;

    @Column(name = "tracker_username", nullable = true)
    private String trackerUsername;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "meeting_stream", joinColumns = @JoinColumn(name = "meeting_id"))
    @Column(name = "stream_id")
    private Set<UUID> streamIds = new HashSet<>();
}
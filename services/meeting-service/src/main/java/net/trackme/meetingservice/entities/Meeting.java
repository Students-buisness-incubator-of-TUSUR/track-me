package net.trackme.meetingservice.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
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

    @Column(name = "team_name", nullable = true)
    private String teamName;

    @Column(name = "tracker_id", nullable = true)
    private String trackerId;

    @Column(name = "tracker_username", nullable = true)
    private String trackerUsername;

    @Column(name = "tracker_full_name", nullable = true)
    private String trackerFullName;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "meeting_stream", joinColumns = @JoinColumn(name = "meeting_id"))
    @Column(name = "stream_id")
    private Set<UUID> streamIds = new HashSet<>();

    @PrePersist
    @PreUpdate
    public void updateTeamStatusValue() {
        if (this.status == MeetingStatus.COMPLETED_AS_NOT_HAPPENED) {
            this.teamStatusValue = BigDecimal.valueOf(-1.0);
            return;
        }

        if (this.status == MeetingStatus.SCHEDULED) {
            this.teamStatusValue = BigDecimal.valueOf(0.0);
            return;
        }

        if (this.teamStatus != null) {
            this.teamStatusValue = switch (this.teamStatus) {
                case OK -> BigDecimal.valueOf(1.0);
                case WITH_ISSUES -> BigDecimal.valueOf(0.5);
                case MANY_ISSUES -> BigDecimal.valueOf(0.25);
            };
        } else {
            this.teamStatusValue = BigDecimal.valueOf(0.0);
        }
    }
}
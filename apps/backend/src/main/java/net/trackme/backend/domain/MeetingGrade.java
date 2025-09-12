package net.trackme.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "meeting_grade")
public class MeetingGrade {
    @Id
    @Column(
            nullable = false,
            updatable = false)
    @GeneratedValue
    @UuidGenerator
    private UUID id;
    private UUID meetingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_card_id")
    private TeamCard teamCard;
    private BigDecimal grade = BigDecimal.ZERO;
}
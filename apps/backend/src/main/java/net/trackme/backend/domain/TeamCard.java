package net.trackme.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import net.trackme.backend.models.TeamCardStatus;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.*;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "team_card")
public class TeamCard {

    @Id
    @Column(
            nullable = false,
            updatable = false)
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(length = 32)
    @Enumerated(EnumType.STRING)
    private TeamCardStatus status;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String meetingRoomLink;

    @ManyToMany(
            fetch = FetchType.EAGER,
            cascade = CascadeType.DETACH)
    @JoinTable(
            name = "team_card_nti_market",
            joinColumns = @JoinColumn(name = "team_card_id"),
            inverseJoinColumns = @JoinColumn(name = "nti_market_id"))
    @Builder.Default
    private List<NTIMarket> ntiMarkets = new ArrayList<>();

    @Column(nullable = false, name = "readiness_level")
    @Enumerated(EnumType.STRING)
    private ReadinessLevel readinessLevel;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
    @JoinTable(
            name = "stream_team_card",
            joinColumns = @JoinColumn(name = "team_id"),
            inverseJoinColumns = @JoinColumn(name = "stream_id")
    )
    @OrderBy("startDate DESC")
    @Builder.Default
    private Set<Stream> streams = new HashSet<>();

    @Column(name = "average_grade", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageGrade = BigDecimal.ZERO;

    @Builder.Default
    private Integer meetingsCount = 0;

    @Builder.Default
    private Integer meetingsCompletedCount = 0;

    @Builder.Default
    private Integer meetingsNotHappenedCount = 0;

    @Builder.Default
    private Integer meetingsCompletedAsNotHappenedCount = 0;

    @Builder.Default
    @OneToMany(
            mappedBy = "teamCard",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    private Set<MeetingGrade> meetingGrades = new HashSet<>();

    public void addStream(Stream stream) {
        streams.clear();
        streams.add(stream);
    }

    public boolean isActive() {
        return streams.stream()
                .allMatch(Stream::isActive);
    }

    public void increaseMeetingCount() {
        if (meetingsCount == null) {
            meetingsCount = 0;
        }
        meetingsCount++;
    }

    public void increaseMeetingCompletedCount() {
        if (meetingsCompletedCount == null) {
            meetingsCompletedCount = 0;
        }
        meetingsCompletedCount++;
    }

    public void addMeetingGrade(MeetingGrade meetingGrade) {
        this.meetingGrades.add(meetingGrade);
        meetingGrade.setTeamCard(this);
    }

    public void addMeetingGrade(UUID meetingId) {
        var meetingGrade = new MeetingGrade();
        meetingGrade.setMeetingId(meetingId);
        meetingGrade.setGrade(null);
        this.addMeetingGrade(meetingGrade);
    }
}

package net.akarmanov.projectplace.domain;

import jakarta.persistence.*;
import lombok.*;
import net.akarmanov.projectplace.models.TeamCardStatus;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    @Column(nullable = false,
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

    @ManyToMany(fetch = FetchType.EAGER,
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

    @OneToMany(mappedBy = "teamCard",
            cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Meeting> teamMeetings = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
    @JoinTable(
            name = "stream_team_card",
            joinColumns = @JoinColumn(name = "team_id"),
            inverseJoinColumns = @JoinColumn(name = "stream_id")
    )
    @OrderBy("startDate DESC")
    @Builder.Default
    private Set<Stream> streams = new HashSet<>();

    @Formula("""
            (SELECT AVG(
                            CASE m.status
                                WHEN 'OK' THEN 1.0
                                WHEN 'WITH_ISSUES' THEN 0.5
                                WHEN 'MANY_ISSUES' THEN 0.25
                                ELSE 0.0
                                END)
             FROM meeting m
             WHERE m.team_id = id)
            """)
    private BigDecimal averageGrade;

    public void addStream(Stream stream) {
        streams.clear();
        streams.add(stream);
    }

    public BigDecimal getAverageGrade() {
        return averageGrade != null
                ? averageGrade.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }
}
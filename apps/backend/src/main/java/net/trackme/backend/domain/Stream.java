package net.trackme.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "stream")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Stream {

    @Id
    @Column(
            nullable = false,
            updatable = false)
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column
    private LocalDate startDate;

    @Column
    private LocalDate endDate;

    @Column(name = "track_start_date")
    private LocalDate trackStartDate;

    private String description;

    @Column(name = "meetings_count")
    @Builder.Default
    private Integer meetingsCount = 10;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
    @JoinTable(
            name = "stream_nti_market",
            joinColumns = @JoinColumn(name = "stream_id"),
            inverseJoinColumns = @JoinColumn(name = "nti_market_id")
    )
    @Builder.Default
    private Set<NTIMarket> ntiMarkets = new HashSet<>();

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image")
    private byte[] imageBytes;

    @ManyToMany(mappedBy = "streams")
    @Builder.Default
    private Set<TeamCard> teamCards = new HashSet<>();

    public void addNtiMarkets(List<NTIMarket> ntiMarkets) {
        this.ntiMarkets.addAll(ntiMarkets);
    }

    public void updateNtiMarkets(List<NTIMarket> ntiMarkets) {
        this.ntiMarkets.clear();
        this.ntiMarkets.addAll(ntiMarkets);
    }

    public boolean isActive() {
        var today = LocalDate.now();
        return (startDate == null || startDate.isBefore(today)) &&
               (endDate == null || endDate.isAfter(today));
    }
}
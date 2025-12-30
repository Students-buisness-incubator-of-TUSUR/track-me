package net.trackme.backend.domain;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
        return (startDate == null || !startDate.isAfter(today))
                && (endDate == null || !endDate.isBefore(today));
    }
}

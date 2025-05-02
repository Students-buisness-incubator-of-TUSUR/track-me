package net.akarmanov.projectplace.domain;

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
  @Column(nullable = false,
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

  @Column(nullable = false)
  @Builder.Default
  private Boolean active = false;

  @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
  @JoinTable(
      name = "stream_nti_market",
      joinColumns = @JoinColumn(name = "stream_id"),
      inverseJoinColumns = @JoinColumn(name = "nti_market_id")
  )
  @Builder.Default
  private Set<NTIMarket> ntiMarkets = new HashSet<>();

  private String description;

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
}
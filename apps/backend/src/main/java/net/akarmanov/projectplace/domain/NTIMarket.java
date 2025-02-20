package net.akarmanov.projectplace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "nti_market")
public class NTIMarket {
  @Id
  @Column(nullable = false,
          updatable = false)
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  private String name;

  private String displayName;

  @ManyToMany
  @JoinTable(
      name = "stream_nti_market",
      joinColumns = @JoinColumn(name = "nti_market_id"),
      inverseJoinColumns = @JoinColumn(name = "stream_id")
  )
  @Builder.Default
  private Set<Stream> streams = new HashSet<>();

  @OneToMany(fetch = LAZY, mappedBy = "ntiMarket")
  @Builder.Default
  private Set<TeamCard> teamCards = new HashSet<>();
}

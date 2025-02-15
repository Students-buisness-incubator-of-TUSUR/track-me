package net.akarmanov.projectplace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
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

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "stream_nti_market",
      joinColumns = @JoinColumn(name = "stream_id"),
      inverseJoinColumns = @JoinColumn(name = "nti_market_id")
  )
  @Builder.Default
  private Set<NTIMarket> ntiMarkets = new HashSet<>();

  @Column
  @Enumerated(EnumType.STRING)
  private ReadinessLevel readinessLevel;

  @ManyToMany(mappedBy = "streams")
  @Builder.Default
  private Set<TeamCard> teamCards = new HashSet<>();

  @ManyToMany(mappedBy = "streams")
  @Builder.Default
  private Set<User> users = new HashSet<>();

  public void addTeamCard(TeamCard teamCard) {
    teamCards.add(teamCard);
  }

  public void addUser(User user) {
    users.add(user);
  }
}
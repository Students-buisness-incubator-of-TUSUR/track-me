package net.akarmanov.projectplace.domain;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.akarmanov.projectplace.models.TeamCardStatus;
import org.hibernate.annotations.UuidGenerator;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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

  @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
  @JoinColumn(name = "nti_market_id", nullable = false)
  private NTIMarket ntiMarket;

  @Column(nullable = false, name = "readiness_level")
  @Enumerated(EnumType.STRING)
  private ReadinessLevel readinessLevel;

  @OneToMany(mappedBy = "teamCard",
             cascade = CascadeType.ALL)
  @Builder.Default
  private Set<Meeting> teamMeetings = new HashSet<>();

  @ManyToMany
  @JoinTable(
      name = "stream_team_card",
      joinColumns = @JoinColumn(name = "team_id"),
      inverseJoinColumns = @JoinColumn(name = "stream_id")
  )
  @Builder.Default
  private Set<Stream> streams = new HashSet<>();

  public void addStream(Stream stream) {
    streams.clear();
    streams.add(stream);
  }
}
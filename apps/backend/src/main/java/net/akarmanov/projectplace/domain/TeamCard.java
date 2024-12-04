package net.akarmanov.projectplace.domain;

import jakarta.persistence.*;
import lombok.*;
import net.akarmanov.projectplace.models.TeamCardStatus;
import org.hibernate.annotations.UuidGenerator;

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
    @Column(nullable = false, updatable = false)
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    private Boolean enabled;

    @Column(length = 32)
    @Enumerated(EnumType.STRING)
    private TeamCardStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "teamCard", cascade = CascadeType.ALL)
    private Set<Meeting> teamMeetings;

    @ManyToMany
    @JoinTable(
            name = "stream_team_card",
            joinColumns = @JoinColumn(name = "team_id"),
            inverseJoinColumns = @JoinColumn(name = "stream_id")
    )
    private Set<Stream> streams;
}
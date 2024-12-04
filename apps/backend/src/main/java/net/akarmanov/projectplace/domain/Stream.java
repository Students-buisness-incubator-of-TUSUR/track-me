package net.akarmanov.projectplace.domain;

import jakarta.persistence.*;
import lombok.*;
import net.akarmanov.projectplace.models.NTIMarket;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "streams")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Stream {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column
    private LocalDate startDate;

    @Column
    private LocalDate endDate;

    @Column
    private NTIMarket ntiMarket;

    @ManyToMany(mappedBy = "streams")
    private Set<TeamCard> teamCards = new HashSet<>();

    @ManyToMany(mappedBy = "streams")
    private Set<User> users = new HashSet<>();

    public void addTeamCard(TeamCard teamCard) {
        teamCards.add(teamCard);
    }

    public void addUser(User user) {
        users.add(user);
    }
}
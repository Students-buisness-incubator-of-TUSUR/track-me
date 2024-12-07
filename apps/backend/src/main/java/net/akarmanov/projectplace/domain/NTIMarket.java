package net.akarmanov.projectplace.domain;

import jakarta.persistence.*;
import lombok.*;
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
@Table(name = "nti_market")
public class NTIMarket {
    @Id
    @Column(nullable = false, updatable = false)
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
    private Set<Stream> streams = new HashSet<>();
}

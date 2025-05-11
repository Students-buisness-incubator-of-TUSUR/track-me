package net.akarmanov.projectplace.domain.spec;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.akarmanov.projectplace.domain.Meeting;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MeetingSpecification {

    public static Specification<Meeting> teamCardIdEquals(UUID teamCardId) {
        return (root, query, criteriaBuilder) -> {
            var teamCardJoin = root.join("teamCard");
            return criteriaBuilder.equal(teamCardJoin.get("id"), teamCardId);
        };
    }

    public static Specification<Meeting> meetingIdEquals(UUID meetingId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("id"), meetingId);
    }
}

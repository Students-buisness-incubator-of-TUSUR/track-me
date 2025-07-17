package net.trackme.meetingservice.entities;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MeetingSpecification {

    public static Specification<Meeting> teamCardIdEquals(UUID teamCardId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("teamCardId"), teamCardId);
    }

    public static Specification<Meeting> meetingIdEquals(UUID meetingId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("id"), meetingId);
    }
}

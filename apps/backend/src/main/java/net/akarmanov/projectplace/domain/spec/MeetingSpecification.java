package net.akarmanov.projectplace.domain.spec;

import net.akarmanov.projectplace.domain.Meeting;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class MeetingSpecification {
  public static Specification<Meeting> userEquals(String userId) {
    return (root, query, criteriaBuilder) -> {
      var teamCardJoin = root.join("teamCard");
      var userJoin = teamCardJoin.join("user");
      return criteriaBuilder.equal(userJoin.get("telegramId"), userId);
    };
  }

  public static Specification<Meeting> teamCardIdEquals(UUID teamCardId) {
    return (root, query, criteriaBuilder) -> {
      var teamCardJoin = root.join("teamCard");
      return criteriaBuilder.equal(teamCardJoin.get("id"), teamCardId);
    };
  }

  public static Specification<Meeting> meetingIdEquals(UUID meetingId) {
    return (root, query, criteriaBuilder) -> {
      return criteriaBuilder.equal(root.get("id"), meetingId);
    };
  }
}

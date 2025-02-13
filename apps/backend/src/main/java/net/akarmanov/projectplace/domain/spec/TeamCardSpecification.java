package net.akarmanov.projectplace.domain.spec;

import net.akarmanov.projectplace.domain.TeamCard;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class TeamCardSpecification {
  public static Specification<TeamCard> nameLike(String name) {
    return (root, query, builder) -> {
      if (name == null) {
        return builder.conjunction();
      }
      return builder.like(builder.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    };
  }

  public static Specification<TeamCard> statusEquals(String status) {
    return (root, query, criteriaBuilder) -> {
      if (status == null) {
        return criteriaBuilder.conjunction();
      }
      return criteriaBuilder.equal(root.get("status"), status);
    };
  }

  public static Specification<TeamCard> userEquals(UUID userId) {
    return (root, query, criteriaBuilder) -> {
      var userJoin = root.join("user");
      return criteriaBuilder.equal(userJoin.get("id"), userId);
    };
  }
}

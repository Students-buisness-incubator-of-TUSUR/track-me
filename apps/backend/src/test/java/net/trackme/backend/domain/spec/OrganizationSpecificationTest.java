package net.trackme.backend.domain.spec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.UUID;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.domain.TeamCard;
import org.junit.jupiter.api.Test;

/**
 * Тесты спецификаций фильтрации по организации.
 */
class OrganizationSpecificationTest {

  private final CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);

  private final CriteriaQuery<?> query = mock(CriteriaQuery.class);

  private final Predicate predicate = mock(Predicate.class);

  @Test
  void streamOrganizationEqualsBuildsPredicate() {
    var organizationId = UUID.randomUUID();
    Root<Stream> root = mock(Root.class);
    @SuppressWarnings("unchecked")
    Path<Object> organizationPath = mock(Path.class);
    when(root.get("organizationId")).thenReturn(organizationPath);
    when(criteriaBuilder.equal(organizationPath, organizationId)).thenReturn(predicate);

    var result = StreamSpecification.organizationEquals(organizationId)
        .toPredicate(root, query, criteriaBuilder);

    assertThat(result).isSameAs(predicate);
  }

  @Test
  void teamCardOrganizationEqualsBuildsPredicate() {
    var organizationId = UUID.randomUUID();
    Root<TeamCard> root = mock(Root.class);
    @SuppressWarnings("unchecked")
    Path<Object> organizationPath = mock(Path.class);
    when(root.get("organizationId")).thenReturn(organizationPath);
    when(criteriaBuilder.equal(organizationPath, organizationId)).thenReturn(predicate);

    var result = TeamCardSpecification.organizationEquals(organizationId)
        .toPredicate(root, query, criteriaBuilder);

    assertThat(result).isSameAs(predicate);
  }

  @Test
  void teamCardOrganizationNameEqualsBuildsPredicate() {
    Root<TeamCard> root = mock(Root.class);
    @SuppressWarnings("unchecked")
    Path<Object> organizationPath = mock(Path.class);
    when(root.get("organizationName")).thenReturn(organizationPath);
    when(criteriaBuilder.equal(organizationPath, "Организация А")).thenReturn(predicate);

    var result = TeamCardSpecification.organizationNameEquals("Организация А")
        .toPredicate(root, query, criteriaBuilder);

    assertThat(result).isSameAs(predicate);
  }
}

package net.trackme.meetingservice.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Тесты спецификаций фильтрации встреч по организации.
 */
class MeetingSpecificationTest {

  private final CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);

  private final CriteriaQuery<?> query = mock(CriteriaQuery.class);

  private final Predicate predicate = mock(Predicate.class);

  @Test
  void organizationEqualsBuildsPredicate() {
    var organizationId = UUID.randomUUID();
    Root<Meeting> root = mock(Root.class);
    @SuppressWarnings("unchecked")
    Path<Object> organizationPath = mock(Path.class);
    when(root.get("organizationId")).thenReturn(organizationPath);
    when(criteriaBuilder.equal(organizationPath, organizationId)).thenReturn(predicate);

    var result = MeetingSpecification.organizationEquals(organizationId)
        .toPredicate(root, query, criteriaBuilder);

    assertThat(result).isSameAs(predicate);
  }
}

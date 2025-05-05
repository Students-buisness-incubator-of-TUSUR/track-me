package net.akarmanov.projectplace.commons.filters;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilterTest {

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private Root<?> root;

    @Mock
    private Path<Object> path;

    @Mock
    private Predicate predicate;

    @Test
    void testToPredicateWithEqualOperator() {
        when(root.get("name")).thenReturn(path);
        when(criteriaBuilder.equal(path.as(String.class), "John Doe")).thenReturn(predicate);

        Filter filter = Filter.builder()
                .fieldName("name")
                .type(OperationType.EQUAL)
                .singleValue("John Doe")
                .build();

        Predicate result = filter.toPredicate(root, criteriaBuilder);

        assertNotNull(result);
        assertEquals(predicate, result);
    }

    @Test
    void testToPredicateWithLikeOperator() {
        when(root.get("description")).thenReturn(path);
        when(criteriaBuilder.like(criteriaBuilder.lower(path.as(String.class)), "%sample%"))
                .thenReturn(predicate);

        Filter filter = Filter.builder()
                .fieldName("description")
                .type(OperationType.LIKE)
                .singleValue("Sample")
                .build();

        Predicate result = filter.toPredicate(root, criteriaBuilder);

        assertNotNull(result);
        assertEquals(predicate, result);
    }

    @Test
    void testToPredicateWithStartDateFieldAndEqualOperator() {
        when(root.get("startDate")).thenReturn(path);
        when(criteriaBuilder.between(eq(path.as(LocalDate.class)), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(predicate);

        Filter filter = Filter.builder()
                .fieldName(Filter.START_DATE_FIELD)
                .type(OperationType.EQUAL)
                .singleValue("2023")
                .build();

        Predicate result = filter.toPredicate(root, criteriaBuilder);

        assertNotNull(result);
        assertEquals(predicate, result);
    }
}
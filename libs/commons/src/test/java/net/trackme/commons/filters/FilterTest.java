package net.trackme.commons.filters;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FilterTest {

    @Mock
    private Root<?> root;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path<Object> path;

    @Test
    void testToPredicateComparisonGreaterThan() {
        Filter filter = Filter.builder()
                .fieldName("numericField")
                .type(OperationType.GREATER_THAN)
                .singleValue("10")
                .build();

        Predicate predicate = mock(Predicate.class);
        when(cb.greaterThan(any(), any(Integer.class))).thenReturn(predicate);
        doReturn(Integer.class).when(path).getJavaType();

        when(root.get("numericField")).thenReturn(path);

        Predicate result = filter.toPredicate(root, cb);
        assertNotNull(result);
        verify(cb).greaterThan(any(), eq(10));
    }

    @Test
    void testToPredicateComparisonLessThan() {
        Filter filter = Filter.builder()
                .fieldName("numericField")
                .type(OperationType.LESS_THAN)
                .singleValue("20")
                .build();

        Predicate predicate = mock(Predicate.class);
        doReturn(predicate).when(cb).lessThan(any(), any(Integer.class));
        doReturn(Integer.class).when(path).getJavaType();

        when(root.get("numericField")).thenReturn(path);

        Predicate result = filter.toPredicate(root, cb);
        assertNotNull(result);
        verify(cb).lessThan(any(), eq(20));
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(root.get(anyString())).thenReturn(path);
    }

    @Test
    void testToPredicateYearFiltering() {
        Filter filter = Filter.builder()
                .fieldName("someField.year")
                .type(OperationType.EQUALS)
                .values(List.of("2022"))
                .build();

        Predicate predicate = mock(Predicate.class);
        doReturn(LocalDate.class).when(path).getJavaType();
        when(cb.equal(any(), any(LocalDate.class))).thenReturn(predicate);
        when(cb.between(any(), any(LocalDate.class), any(LocalDate.class))).thenReturn(predicate);
        when(cb.or(any())).thenReturn(predicate);

        Predicate result = filter.toPredicate(root, cb);
        assertNotNull(result);
        verify(cb, times(1)).between(any(), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void testToPredicateEquals() {
        Filter filter = Filter.builder()
                .fieldName("testField")
                .type(OperationType.EQUALS)
                .singleValue("testValue")
                .build();

        Predicate predicate = mock(Predicate.class);
        when(cb.equal(path, "testValue")).thenReturn(predicate);
        doReturn(String.class).when(path).getJavaType();

        Predicate result = filter.toPredicate(root, cb);
        assertNotNull(result);
        assertEquals(predicate, result);
        verify(cb).equal(path, "testValue");
    }

    @Test
    void testToPredicateIn() {
        Filter filter = Filter.builder()
                .fieldName("testField")
                .type(OperationType.IN)
                .values(List.of("value1", "value2"))
                .build();

        Predicate predicate = mock(Predicate.class);
        when(path.in(List.of("value1", "value2"))).thenReturn(predicate);
        doReturn(String.class).when(path).getJavaType();

        Predicate result = filter.toPredicate(root, cb);
        assertNotNull(result);
        assertEquals(predicate, result);
        verify(path).in(List.of("value1", "value2"));
    }

    @Test
    void testToPredicateInvalidFieldName() {
        Filter filter = Filter.builder()
                .fieldName(null)
                .type(OperationType.EQUALS)
                .singleValue("testValue")
                .build();

        Exception exception = assertThrows(IllegalArgumentException.class, () -> filter.toPredicate(root, cb));
        assertEquals("Field name cannot be null or blank", exception.getMessage());
    }

    @Test
    void testToPredicateNoValues() {
        Filter filter = Filter.builder()
                .fieldName("testField")
                .type(OperationType.EQUALS)
                .build();

        Exception exception = assertThrows(IllegalArgumentException.class, () -> filter.toPredicate(root, cb));
        assertEquals("No values provided for filter", exception.getMessage());
    }
}
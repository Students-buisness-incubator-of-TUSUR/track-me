package net.trackme.backend.domain.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.domain.TeamCard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Общий порядок команд: поток по дате окончания, рейтинг, кириллица перед латиницей. */
public final class TeamCardOrdering {
    public static final String SORT_PROPERTY = "streamEndDate";

    private static final String RUSSIAN_LOWER = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя";
    private static final String LATIN_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private TeamCardOrdering() {
    }

    /**
     * Формирует SQL-порядок до пагинации. Для старых карточек с несколькими потоками
     * выбирает поток с наиболее поздним окончанием, при равенстве — с меньшим ID.
     */
    public static Ordering byStreamEndDate(Root<TeamCard> root, CriteriaQuery<?> query,
                                           CriteriaBuilder cb) {
        var stream = root.<TeamCard, Stream>join("streams");
        var endDate = stream.<LocalDate>get("endDate");
        var streamId = stream.<UUID>get("id");
        var undated = cb.<Integer>selectCase().when(cb.isNull(endDate), 1).otherwise(0);
        var grade = cb.coalesce(root.<BigDecimal>get("averageGrade"), BigDecimal.ZERO);
        var name = cb.function("translate", String.class, cb.trim(root.get("name")),
                cb.literal(UPPER), cb.literal(RUSSIAN_LOWER + LATIN_LOWER));
        var firstLetter = cb.substring(cb.function("regexp_replace", String.class, name,
                cb.literal("^[^а-яёa-z]*"), cb.literal("")), 1, 1);
        var script = cb.<Integer>selectCase()
                .when(cb.and(cb.notEqual(firstLetter, ""),
                        cb.gt(cb.locate(cb.literal(RUSSIAN_LOWER), firstLetter), 0)), 0)
                .when(cb.and(cb.notEqual(firstLetter, ""),
                        cb.gt(cb.locate(cb.literal(LATIN_LOWER), firstLetter), 0)), 1)
                .otherwise(2);
        // Байтовый ключ не зависит от локали PostgreSQL и сохраняет место буквы Ё.
        var alphabeticName = cb.function("translate", String.class, name,
                cb.literal(RUSSIAN_LOWER), cb.literal("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefg"));
        var nameKey = cb.function("convert_to", byte[].class, alphabeticName, cb.literal("UTF8"));
        var id = root.<UUID>get("id");

        var betterStream = query.subquery(UUID.class);
        var candidate = betterStream.correlate(root).<TeamCard, Stream>join("streams");
        var candidateEnd = candidate.<LocalDate>get("endDate");
        var sameEnd = cb.or(cb.equal(candidateEnd, endDate),
                cb.and(cb.isNull(candidateEnd), cb.isNull(endDate)));
        betterStream.select(candidate.get("id")).where(cb.or(
                cb.and(cb.isNull(endDate), cb.isNotNull(candidateEnd)),
                cb.greaterThan(candidateEnd, endDate),
                cb.and(sameEnd, cb.lessThan(candidate.<UUID>get("id"), streamId))));

        return new Ordering(cb.not(cb.exists(betterStream)),
                List.of(undated, endDate, streamId, grade, script, nameKey, id),
                List.of(cb.asc(undated), cb.desc(endDate), cb.asc(streamId), cb.desc(grade),
                        cb.asc(script), cb.asc(nameKey), cb.asc(id)));
    }

    public record Ordering(Predicate selectedStream, List<Expression<?>> keys, List<Order> orders) {
    }
}

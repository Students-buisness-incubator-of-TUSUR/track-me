package net.trackme.backend.repositories;

import jakarta.persistence.EntityManager;
import net.trackme.backend.domain.MeetingGrade;
import net.trackme.backend.domain.NTIMarket;
import net.trackme.backend.domain.ReadinessLevel;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.repos.TeamCardsRepositoryImpl;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Выполняет реальные Criteria-запросы; PostgreSQL convert_to эмулируется в H2. */
public class TeamCardStreamOrderingTest {
    private static SessionFactory factory;
    private EntityManager entityManager;
    private TeamCardsRepositoryImpl repository;

    @BeforeAll
    static void createDatabase() {
        var url = System.getProperty("sbi932.jdbcUrl", "jdbc:h2:mem:team-stream-order;DB_CLOSE_DELAY=-1");
        var h2 = url.startsWith("jdbc:h2:");
        factory = new Configuration()
                .addAnnotatedClass(TeamCard.class).addAnnotatedClass(Stream.class)
                .addAnnotatedClass(NTIMarket.class).addAnnotatedClass(MeetingGrade.class)
                .setProperty("hibernate.connection.driver_class", h2 ? "org.h2.Driver" : "org.postgresql.Driver")
                .setProperty("hibernate.connection.url", url)
                .setProperty("hibernate.connection.username", System.getProperty("sbi932.jdbcUser", "sa"))
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.query.fail_on_pagination_over_collection_fetch", "true")
                .buildSessionFactory();
        if (h2) {
            try (var manager = factory.createEntityManager()) {
                manager.getTransaction().begin();
                manager.createNativeQuery("CREATE ALIAS CONVERT_TO FOR "
                        + "'net.trackme.backend.repositories.TeamCardStreamOrderingTest.convertTo'")
                        .executeUpdate();
                manager.getTransaction().commit();
            }
        }
    }

    public static byte[] convertTo(String value, String encoding) {
        return value.getBytes(Charset.forName(encoding));
    }

    @BeforeEach
    void setUp() {
        entityManager = factory.createEntityManager();
        entityManager.getTransaction().begin();
        repository = new TeamCardsRepositoryImpl();
        ReflectionTestUtils.setField(repository, "entityManager", entityManager);
    }

    @AfterEach
    void tearDown() {
        entityManager.getTransaction().rollback();
        entityManager.close();
    }

    @AfterAll
    static void closeDatabase() {
        factory.close();
    }

    @Test
    void exampleRemainsOrderedAcrossPageBoundariesAndReload() {
        var recent = stream("Поток 3", "2026-12-30");
        recent.setStartDate(LocalDate.of(2025, 1, 1));
        var earlier = stream("Поток 2", "2026-06-30");
        earlier.setStartDate(LocalDate.of(2026, 6, 1));
        team("Омега", "0.95", earlier);
        team("Alpha", "0.90", recent);
        team("Вега", "0.90", recent);
        team("Зенит", "0.75", recent);
        team("Берёза", "0.90", recent);
        team("Команда без потока", "1.00");
        entityManager.flush();
        entityManager.clear();

        List<String> names = new ArrayList<>();
        for (int pageNumber = 0; pageNumber < 3; pageNumber++) {
            var page = repository.findAllByStreamOrder(null, PageRequest.of(pageNumber, 2));
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(3);
            names.addAll(page.stream().map(TeamCard::getName).toList());
            entityManager.clear();
        }

        assertThat(names).containsExactly("Берёза", "Вега", "Alpha", "Зенит", "Омега");
        assertThat(allNames()).containsExactlyElementsOf(names);
    }

    @Test
    void namesUseRussianAlphabetIncludingYoThenLatinIgnoringCase() {
        var stream = stream("Поток", "2026-12-30");
        for (var name : List.of("zebra", "Жук", "Beta", "Яблоко", "Ёж", "Енот", "alpha", "альфа")) {
            team(name, "0.90", stream);
        }
        team("Zulu с высоким рейтингом", "0.99", stream);
        entityManager.flush();

        assertThat(allNames()).containsExactly("Zulu с высоким рейтингом", "альфа", "Енот", "Ёж",
                "Жук", "Яблоко", "alpha", "Beta", "zebra");
    }

    @Test
    void streamsWithSameEndDateStaySeparateEvenWithSameName() {
        var first = stream("Одинаковый", "2026-12-30");
        var second = stream("Одинаковый", "2026-12-30");
        var streams = java.util.stream.Stream.of(first, second)
                .sorted(Comparator.comparing(stream -> stream.getId().toString())).toList();
        team("Первая группа низкий", "0.10", streams.getFirst());
        team("Вторая группа высокий", "0.99", streams.getLast());
        team("Первая группа высокий", "0.20", streams.getFirst());
        team("Вторая группа низкий", "0.90", streams.getLast());
        entityManager.flush();

        assertThat(allNames()).containsExactly("Первая группа высокий", "Первая группа низкий",
                "Вторая группа высокий", "Вторая группа низкий");
    }

    @Test
    void legacyMultipleStreamsChooseLatestAndDoNotDuplicateFilteredPages() {
        var undated = stream("Без даты", null);
        var earlier = stream("Старый", "2026-06-30");
        var latest = stream("Новый", "2026-12-30");
        team("Ранняя команда", "0.95", earlier);
        team("Последняя команда", "1.00", undated);
        team("Несколько потоков", "0.10", latest, earlier, undated);
        entityManager.flush();
        entityManager.clear();
        Specification<TeamCard> filter = (root, query, cb) ->
                root.join("streams").get("id").in(earlier.getId(), latest.getId(), undated.getId());

        var first = repository.findAllByStreamOrder(filter, PageRequest.of(0, 2));
        var second = repository.findAllByStreamOrder(filter, PageRequest.of(1, 2));

        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(java.util.stream.Stream.concat(first.stream(), second.stream()).map(TeamCard::getName))
                .containsExactly("Несколько потоков", "Ранняя команда", "Последняя команда");
    }

    @Test
    void identicalNamesAndGradesHaveStableIdsAcrossPages() {
        var stream = stream("Поток", "2026-12-30");
        var teams = List.of(team("Команда", "0.90", stream), team("Команда", "0.90", stream),
                team("Команда", "0.90", stream));
        entityManager.flush();
        entityManager.clear();
        var expectedIds = teams.stream().map(TeamCard::getId)
                .sorted(Comparator.comparing(Object::toString)).toList();

        for (int page = 0; page < 3; page++) {
            assertThat(repository.findAllByStreamOrder(null, PageRequest.of(page, 1)).getContent())
                    .extracting(TeamCard::getId).containsExactly(expectedIds.get(page));
            entityManager.clear();
        }
    }

    @Test
    void changedGradeAndStreamEndDateAreUsedOnNextRead() {
        var older = stream("Старый", "2026-06-30");
        var recent = stream("Новый", "2026-12-30");
        var changed = team("Изменённая", "0.10", recent);
        team("Лидер", "0.90", recent);
        team("Старая", "0.99", older);
        entityManager.flush();
        assertThat(allNames()).containsExactly("Лидер", "Изменённая", "Старая");

        changed.setAverageGrade(new BigDecimal("0.95"));
        entityManager.flush();
        assertThat(allNames()).containsExactly("Изменённая", "Лидер", "Старая");

        older.setEndDate(LocalDate.of(2027, 1, 1));
        entityManager.flush();
        entityManager.clear();
        assertThat(allNames()).containsExactly("Старая", "Изменённая", "Лидер");
    }

    @Test
    void passiveAndEnabledDoNotOverrideRatingAndNullGradeActsAsZero() {
        var stream = stream("Поток", "2026-12-30");
        var passive = team("Отчисленная", "0.99", stream);
        passive.setPassive(true);
        passive.setEnabled(false);
        team("Активная", "0.50", stream);
        team("Нулевая", "0", stream);
        team("Без рейтинга", null, stream);
        entityManager.flush();

        assertThat(allNames()).containsExactly("Отчисленная", "Активная", "Без рейтинга", "Нулевая");
    }

    private List<String> allNames() {
        Page<TeamCard> page = repository.findAllByStreamOrder(null, Pageable.unpaged());
        return page.stream().map(TeamCard::getName).toList();
    }

    private Stream stream(String name, String endDate) {
        var stream = Stream.builder().name(name)
                .endDate(endDate == null ? null : LocalDate.parse(endDate)).build();
        entityManager.persist(stream);
        return stream;
    }

    private TeamCard team(String name, String grade, Stream... streams) {
        var team = TeamCard.builder().name(name).username("tracker").meetingRoomLink("https://example.com")
                .readinessLevel(ReadinessLevel.LEVEL_1).streams(Set.of(streams))
                .averageGrade(grade == null ? null : new BigDecimal(grade)).build();
        entityManager.persist(team);
        return team;
    }
}

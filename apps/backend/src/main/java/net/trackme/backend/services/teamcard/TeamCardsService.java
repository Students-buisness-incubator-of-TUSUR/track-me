package net.trackme.backend.services.teamcard;

import net.trackme.backend.domain.TeamCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

public interface TeamCardsService {
    /**
     * Создать карточку команды.
     * @param createTeamCardDto Dto команды для создания
     * @return Карточка команды
     */
    TeamCard createTeamCard(TeamCard createTeamCardDto);

    /**
     * Обновить карточку команды.
     * @param teamCardId Идентификатор команды
     * @param updateTeamCardDto Dto команды для обновления
     * @return Карточка команды
     */
    TeamCard updateTeamCard(UUID teamCardId, TeamCard updateTeamCardDto);

    /**
     * Получить пагинированный список карточек команд.
     * @param specification Фильтрация
     * @param pageable Пагинация
     * @return Страница команд
     */
    Page<TeamCard> getTeamCards(Specification<TeamCard> specification, Pageable pageable);

    /**
     * Получить карточку команды.
     * @param id Идентификатор команды
     * @return Карточка команды
     */
    TeamCard getTeamCard(UUID id);

    /**
     * Удалить карточку команды.
     * @param id Идентификатор команды
     */
    void deleteTeamCard(UUID id);

    /**
     * Создать карточку команды.
     * @param teamCard Карточка команды
     * @param username Имя пользователя
     * @return Карточка команды
     */
    TeamCard createTeamCard(TeamCard teamCard, String username);

    /**
     * Обновить карточку команды.
     * @param teamCardId Идентификатор команды
     * @param teamCard Карточка команды
     * @param streamId Идентификатор потока
     * @param username Имя пользователя
     * @return Карточка команды
     */
    TeamCard updateTeamCard(UUID teamCardId, TeamCard teamCard, UUID streamId, String username);

    /**
     * Получить все команды.
     * @param specification Фильтрация
     * @param pageable Пагинация
     * @return Страница команд
     */
    Page<TeamCard> findAll(Specification<TeamCard> specification, Pageable pageable);

    /**
     * Получить карточку команды.
     * @param id Идентификатор команды
     * @param username Имя пользователя
     * @return Карточка команды
     */
    TeamCard getTeamCard(UUID id, String username);

    /**
     * Удалить карточку команды.
     * @param id Идентификатор команды
     * @param username Имя пользователя
     */
    void deleteTeamCard(UUID id, String username);

    /**
     * Создать карточку команды.
     * @param teamCard Карточка команды
     * @param streamId Идентификатор потока
     * @param username Имя пользователя
     * @return Карточка команды
     */
    TeamCard createTeamCard(TeamCard teamCard, UUID streamId, String username);

    /**
     * Получить количество команд в потоке.
     * @param streamId Идентфикатор потока
     * @return Количество команд в потоке
     */
    Integer getTeamCardCount(UUID streamId);

    /**
     * Получить список названий команд пользователя.
     * @param username Имя пользователя
     * @return Список названий команд
     */
    List<String> getTeamCardNamesByUser(String username);

    /**
     * Получить все команды пользователя.
     * @param username Имя пользователя
     * @return Список команд
     */
    List<TeamCard> getTeamCardsByUser(String username);

    /**
     * Переназначить все команды с одного пользователя на другого.
     * Обновляет как username, так и trackerFullName для консистентности данных.
     *
     * @param fromUsername исходный username трекера
     * @param toUsername новый username трекера
     * @param toUserFullName полное имя нового трекера (если известно, иначе используется username)
     */
    void reassignTeams(String fromUsername, String toUsername, String toUserFullName);
}

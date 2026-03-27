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
     * Получить все карточки команд
     * @param specification Фильтрация
     * @return Список карточек команд
     */
    List<TeamCard> getTeamCards(Specification<TeamCard> specification);

    /**
     * Получить пагинированный список карточек команд.
     * @param specification Фильтрация
     * @param pageable Пагинация
     * @return Страница команд
     */
    Page<TeamCard> getTeamCardsPageable(Specification<TeamCard> specification, Pageable pageable);

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
}

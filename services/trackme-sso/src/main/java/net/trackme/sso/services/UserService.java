package net.trackme.sso.services;

import net.trackme.commons.filters.FilterRequest;
import net.trackme.sso.dao.entity.UserEntity;
import net.trackme.sso.dto.RegistrationRequestDto;
import net.trackme.sso.dto.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * Сервис для управления пользователями системы.
 * Предоставляет методы для создания, обновления, блокировки
 * и получения информации о пользователях.
 */
public interface UserService {

    /**
     * Создаёт пользователя на основе регистрационных данных.
     * Пользователь будет не активирован.
     *
     * @param userDto данные, указанные при регистрации
     * @return созданная сущность пользователя
     */
    UserEntity saveUser(RegistrationRequestDto userDto);

    /**
     * Сохраняет сущность пользователя в базе данных.
     *
     * @param userEntity сущность пользователя для сохранения
     */
    void save(UserEntity userEntity);

    /**
     * Изменяет пароль пользователя.
     *
     * @param username имя пользователя
     * @param newPassword новый пароль
     * @param oldPassword старый пароль
     */
    void changePassword(String username, String newPassword,
            String oldPassword);

    /**
     * Сбрасывает пароль пользователя по email.
     *
     * @param email адрес электронной почты пользователя
     * @param password новый пароль
     */
    void resetPassword(String email, String password);

    /**
     * Находит пользователя по имени.
     *
     * @param name имя пользователя
     * @return найденная сущность пользователя
     */
    UserEntity findByUsername(String name);

    /**
     * Находит пользователя по email.
     *
     * @param email адрес электронной почты
     * @return найденная сущность пользователя
     */
    UserEntity findByEmail(String email);

    /**
     * Включает учётную запись пользователя.
     *
     * @param username имя пользователя
     */
    void enableUser(String username);

    /**
     * Отключает учётную запись пользователя.
     *
     * @param username имя пользователя
     */
    void disableUser(String username);

    /**
     * Разблокирует учётную запись пользователя.
     *
     * @param username имя пользователя
     */
    void unlockUser(String username);

    /**
     * Полностью удаляет пользователя из системы.
     *
     * @param username имя пользователя
     */
    void deleteUser(String username);

    /**
     * Получает список команд, в которых состоит пользователь.
     *
     * @param username имя пользователя
     * @return список команд пользователя
     */
    List<Map<String, String>> getUserTeams(String username);

    /**
     * Получает подробную информацию о пользователе.
     *
     * @param username имя пользователя
     * @return DTO с информацией о пользователе
     */
    UserDto getUserInfo(String username);

    /**
     * Получает список трекеров с фильтрацией и пагинацией.
     *
     * @param filterRequest параметры фильтрации
     * @param pageable параметры пагинации
     * @return страница с трекерами
     */
    Page<UserDto> getTrackers(FilterRequest filterRequest,
            Pageable pageable);

    /**
     * Получает список администраторов с фильтрацией и пагинацией.
     *
     * @param filterRequest параметры фильтрации
     * @param pageable параметры пагинации
     * @return страница с администраторами
     */
    Page<UserDto> getAdmins(FilterRequest filterRequest,
            Pageable pageable);

    /**
     * Проверяет существование пользователя по email или имени.
     *
     * @param email адрес электронной почты
     * @param username имя пользователя
     * @return true, если пользователь с таким email или именем существует
     */
    boolean existsByEmailOrUsername(String email, String username);

    /**
     * Проверяет существование пользователя по email.
     *
     * @param email адрес электронной почты
     * @return true, если пользователь с таким email существует
     */
    boolean existsByEmail(String email);
}

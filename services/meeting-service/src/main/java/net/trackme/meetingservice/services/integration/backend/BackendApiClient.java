package net.trackme.meetingservice.services.integration.backend;

import net.trackme.meetingservice.services.integration.backend.dto.TeamCardDto;
import net.trackme.meetingservice.services.integration.backend.exceptions.TeamCardNotFoundException;
import net.trackme.meetingservice.services.integration.exceptions.IntegrationException;

import java.util.UUID;

/**
 * Клиент для взаимодействия с сервисом trackme-backend.
 */
public interface BackendApiClient {
    /**
     * Получает карточку команды по идентификатору.
     *
     * @param id идентификатор карточки команды
     * @return DTO карточки команды
     * @throws TeamCardNotFoundException если карточка не найдена
     * @throws IntegrationException если сервис недоступен
     */
    TeamCardDto getTeamCardById(UUID id);
}

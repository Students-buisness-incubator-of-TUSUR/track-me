package net.trackme.backend.mapping;

import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardCreateOrUpdateDto;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {NtiMarketMapper.class, StreamMapper.class})
public interface TeamCardMapper {
    @Mapping(target = "streams", ignore = true)
    TeamCard mapToEntity(TeamCardDto dto);

    @Mapping(target = "meetingsNotHappenedCount", ignore = true)
    @Mapping(target = "meetingsCount", ignore = true)
    @Mapping(target = "meetingsCompletedCount", ignore = true)
    @Mapping(target = "meetingsCompletedAsNotHappenedCount", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "averageGrade", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "ntiMarkets", ignore = true)
    @Mapping(target = "streams", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(
            target = "readinessLevel",
            expression = "java( ReadinessLevel.fromValue(dto.readinessLevel()) )")
    TeamCard mapToEntity(TeamCardCreateOrUpdateDto dto);

    @Mapping(target = "enabled", expression = "java( entity.isActive() )")
    @Mapping(
            target = "readinessLevel", expression = "java( entity.getReadinessLevel().getValue() )")
    TeamCardDto mapToDto(TeamCard entity);

}

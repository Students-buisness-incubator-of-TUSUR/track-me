package net.akarmanov.projectplace.mapping;

import net.akarmanov.projectplace.domain.TeamCard;
import net.akarmanov.projectplace.rest.api.teamcard.dto.TeamCardCreateOrUpdateDto;
import net.akarmanov.projectplace.rest.api.teamcard.dto.TeamCardDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {NtiMarketMapper.class})
public interface TeamCardMapper {
  @Mapping(target = "teamMeetings", ignore = true)
  @Mapping(target = "streams", ignore = true)
  TeamCard mapToEntity(TeamCardDto dto);

  @Mapping(target = "username", ignore = true)
  @Mapping(target = "ntiMarket", ignore = true)
  @Mapping(target = "teamMeetings", ignore = true)
  @Mapping(target = "streams", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "enabled", ignore = true)
  @Mapping(target = "readinessLevel",
           expression = "java( ReadinessLevel.fromValue(dto.readinessLevel()) )")
  TeamCard mapToEntity(TeamCardCreateOrUpdateDto dto);

  @Mapping(target = "username", source = "username")
  @Mapping(target = "readinessLevel", expression = "java( entity.getReadinessLevel().getValue() )")
  TeamCardDto mapToDto(TeamCard entity);

  @Mapping(target = "username", ignore = true)
  @Mapping(target = "teamMeetings", ignore = true)
  @Mapping(target = "ntiMarket", ignore = true)
  @Mapping(target = "streams", ignore = true)
  @Mapping(target = "enabled", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "status", ignore = true)
  void updateFromDto(TeamCardCreateOrUpdateDto dto, @MappingTarget TeamCard entity);
}

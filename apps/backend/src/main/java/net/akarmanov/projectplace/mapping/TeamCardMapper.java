package net.akarmanov.projectplace.mapping;

import net.akarmanov.projectplace.domain.TeamCard;
import net.akarmanov.projectplace.rest.api.teamcard.dto.TeamCardCreateOrUpdateDto;
import net.akarmanov.projectplace.rest.api.teamcard.dto.TeamCardDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(
    componentModel = "spring",
    uses = {UserMapper.class})
public interface TeamCardMapper {
  @Mapping(target = "user",
           ignore = true)
  TeamCard mapToEntity(TeamCardDto dto);

  TeamCard mapToEntity(TeamCardCreateOrUpdateDto dto);

  @Mapping(target = "userId",
           source = "user.id")
  TeamCardDto mapToDto(TeamCard entity);

  @Mapping(target = "id",
           ignore = true)
  @Mapping(target = "user",
           ignore = true)
  @Mapping(target = "status",
           ignore = true)
  void updateFromDto(TeamCardCreateOrUpdateDto dto, @MappingTarget TeamCard entity);
}

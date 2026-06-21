package net.trackme.backend.mapping;

import net.trackme.backend.domain.NTIMarket;
import net.trackme.backend.rest.api.dto.NTIMarketDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import static org.mapstruct.NullValueCheckStrategy.ALWAYS;
import static org.mapstruct.NullValuePropertyMappingStrategy.IGNORE;

@Mapper(
    componentModel = "spring",
    nullValueCheckStrategy = ALWAYS,
    nullValuePropertyMappingStrategy = IGNORE)
public interface NtiMarketMapper {
  @Mapping(target = "teamCards", ignore = true)
  @Mapping(target = "streams", ignore = true)
  NTIMarket mapToEntity(NTIMarketDto dto);

  NTIMarketDto mapToDto(NTIMarket entity);
}

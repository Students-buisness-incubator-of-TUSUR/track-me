package net.akarmanov.projectplace.mapping;

import net.akarmanov.projectplace.domain.Stream;
import net.akarmanov.projectplace.rest.api.dto.StreamCreateDto;
import net.akarmanov.projectplace.rest.api.dto.StreamDto;
import net.akarmanov.projectplace.rest.api.dto.StreamUpdateDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = {NtiMarketMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface StreamMapper {

  @Mapping(target = "imageBytes", ignore = true)
  @Mapping(target = "teamCards", ignore = true)
  Stream mapFromDto(StreamDto dto);

  StreamDto mapToDto(Stream entity);

  List<Stream> mapFromDto(List<StreamDto> dtos);

  List<StreamDto> mapToDto(List<Stream> entities);

  @Mapping(target = "ntiMarkets", ignore = true)
  @Mapping(target = "imageBytes", ignore = true)
  @Mapping(target = "teamCards", ignore = true)
  @Mapping(target = "startDate", ignore = true)
  @Mapping(target = "id", ignore = true)
  void updateFromDto(StreamUpdateDto dto, @MappingTarget Stream entity);

  @Mapping(target = "active", ignore = true)
  @Mapping(target = "ntiMarkets", ignore = true)
  @Mapping(target = "imageBytes", ignore = true)
  @Mapping(target = "teamCards", ignore = true)
  @Mapping(target = "id", ignore = true)
  Stream mapFromDto(StreamCreateDto dto);
}

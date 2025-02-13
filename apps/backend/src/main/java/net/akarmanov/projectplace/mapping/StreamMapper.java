package net.akarmanov.projectplace.mapping;

import net.akarmanov.projectplace.domain.Stream;
import net.akarmanov.projectplace.rest.api.dto.StreamCreateDto;
import net.akarmanov.projectplace.rest.api.dto.StreamDto;
import net.akarmanov.projectplace.rest.api.dto.StreamUpdateDto;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {NtiMarketMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface StreamMapper {

    @Mapping(target = "users", ignore = true)
    @Mapping(target = "teamCards", ignore = true)
    Stream mapFromDto(StreamDto dto);

    @Mapping(target = "readinessLevel", expression = "java( entity.getReadinessLevel().getValue() )")
    StreamDto mapToDto(Stream entity);

    List<Stream> mapFromDto(List<StreamDto> dtos);

    List<StreamDto> mapToDto(List<Stream> entities);

    @Mapping(target = "users", ignore = true)
    @Mapping(target = "teamCards", ignore = true)
    @Mapping(target = "startDate", ignore = true)
    @Mapping(target = "id", ignore = true)
    void updateFromDto(StreamUpdateDto dto, @MappingTarget Stream entity);

    @Mapping(target = "users", ignore = true)
    @Mapping(target = "teamCards", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "readinessLevel", expression = "java( ReadinessLevel.fromValue(dto.readinessLevel()) )")
    Stream mapFromDto(StreamCreateDto dto);
}

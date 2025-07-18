package net.trackme.backend.mapping;

import net.trackme.backend.domain.Stream;
import net.trackme.backend.rest.api.stream.StreamCreateDto;
import net.trackme.backend.rest.api.stream.StreamDto;
import net.trackme.backend.rest.api.stream.StreamUpdateDto;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        uses = {NtiMarketMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface StreamMapper {

    @Mapping(target = "active", expression = "java( entity.isActive() )")
    StreamDto mapToDto(Stream entity);

    @Mapping(target = "ntiMarkets", ignore = true)
    @Mapping(target = "imageBytes", ignore = true)
    @Mapping(target = "teamCards", ignore = true)
    @Mapping(target = "startDate", ignore = true)
    @Mapping(target = "id", ignore = true)
    void updateFromDto(StreamUpdateDto dto, @MappingTarget Stream entity);


    @Mapping(target = "ntiMarkets", ignore = true)
    @Mapping(target = "imageBytes", ignore = true)
    @Mapping(target = "teamCards", ignore = true)
    @Mapping(target = "id", ignore = true)
    Stream mapFromDto(StreamCreateDto dto);
}

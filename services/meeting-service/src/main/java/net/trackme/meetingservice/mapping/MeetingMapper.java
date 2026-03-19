package net.trackme.meetingservice.mapping;

import net.trackme.meetingservice.api.MeetingCreateDto;
import net.trackme.meetingservice.api.MeetingDto;
import net.trackme.meetingservice.api.MeetingUpdateDto;
import net.trackme.meetingservice.entities.Meeting;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MeetingMapper {
    @Mapping(target = "teamStatus", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "teamCardId", ignore = true)
    @Mapping(target = "imageBytes", ignore = true)
    @Mapping(target = "id", ignore = true)
    Meeting mapToEntity(MeetingCreateDto meetingCreateDto);

    @Mapping(target = "roomLink", ignore = true)
    @Mapping(target = "recordLink", ignore = false)
    MeetingDto mapToDto(Meeting meeting);

    @Mapping(target = "teamCardId", ignore = true)
    @Mapping(target = "imageBytes", ignore = true)
    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(MeetingUpdateDto updateDto, @MappingTarget Meeting meeting);
}

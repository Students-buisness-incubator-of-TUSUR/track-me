package net.trackme.meetingservice.mapping;

import net.trackme.meetingservice.api.MeetingCreateDto;
import net.trackme.meetingservice.api.MeetingDto;
import net.trackme.meetingservice.api.MeetingUpdateDto;
import net.trackme.meetingservice.entities.Meeting;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring")
public interface MeetingMapper {

    @Mapping(target = "screenshot",
            ignore = true)
    @Mapping(target = "id",
            ignore = true)
    Meeting mapToEntity(MeetingCreateDto meetingCreateDto);

    MeetingDto mapToDto(Meeting meeting);


    @Mapping(target = "startDate",
            ignore = true)
    @Mapping(target = "screenshot",
            ignore = true)
    @Mapping(target = "id",
            ignore = true)
    Meeting mapToEntity(MeetingUpdateDto meetingCreateDto);
}

package net.trackme.backend.mapping;

import net.trackme.backend.domain.Meeting;
import net.trackme.backend.rest.api.meeting.MeetingCreateDto;
import net.trackme.backend.rest.api.meeting.MeetingDto;
import net.trackme.backend.rest.api.meeting.MeetingUpdateDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring")
public interface
MeetingMapper {
    @Mapping(target = "teamCard",
            ignore = true)
    @Mapping(target = "screenshot",
            ignore = true)
    @Mapping(target = "id",
            ignore = true)
    Meeting mapToEntity(MeetingCreateDto meetingCreateDto);

    @Mapping(target = "teamCardId",
            source = "teamCard.id")
    MeetingDto mapToDto(Meeting meeting);

    @Mapping(target = "teamCard",
            ignore = true)
    @Mapping(target = "startDate",
            ignore = true)
    @Mapping(target = "screenshot",
            ignore = true)
    @Mapping(target = "id",
            ignore = true)
    Meeting mapToEntity(MeetingUpdateDto meetingCreateDto);
}

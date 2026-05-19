package net.trackme.backend.mapping;

import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardCreateDto;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardDto;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardReportRecordDto;
import net.trackme.backend.rest.api.teamcard.dto.TeamCardUpdateDto;
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
    @Mapping(target = "meetingGrades", ignore = true)
    @Mapping(
            target = "readinessLevel",
            expression = "java( ReadinessLevel.fromValue(dto.readinessLevel()) )")
    TeamCard mapToEntity(TeamCardCreateDto dto);

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
    @Mapping(target = "passive", source = "passive")
    @Mapping(target = "meetingGrades", ignore = true)
    @Mapping(
            target = "readinessLevel",
            expression = "java( dto.readinessLevel() == null ? null : ReadinessLevel.fromValue(dto.readinessLevel()) )")
    TeamCard mapToEntity(TeamCardUpdateDto dto);

    @Mapping(target = "enabled", expression = "java( entity.isActive() )")
    @Mapping(
            target = "readinessLevel", expression = "java( entity.getReadinessLevel().getValue() )")
    @Mapping(target = "passive", source = "passive")
    TeamCardDto mapToDto(TeamCard entity);

    @Mapping(
            target = "streamName",
            expression = "java( entity"
                    + ".getStreams().stream()"
                    + ".map(Stream::getName)"
                    + ".findFirst().orElse(\"\") )")
    @Mapping(
            target = "streamId",
            expression = "java( entity.getStreams().stream()"
                    + ".map(net.trackme.backend.domain.Stream::getId)"
                    + ".findFirst().orElse(null) )")
    @Mapping(
            target = "startDate",
            expression = "java( entity"
                    + ".getStreams().stream()"
                    + ".map(Stream::getStartDate)"
                    + ".findFirst().orElse(null) )")
    @Mapping(
            target = "endDate",
            expression = "java( entity"
                    + ".getStreams().stream()"
                    + ".map(Stream::getEndDate)"
                    + ".findFirst().orElse(null) )")
    @Mapping(target = "teamCardName", expression = "java( entity.getName() )")
    @Mapping(target = "teamId", source = "id")
    @Mapping(target = "username", expression = "java( entity.getUsername() )")
    @Mapping(target = "averageTeamGrade", expression = "java( entity.getAverageGrade() )")
    @Mapping(target = "averageUserGrade", ignore = true)
    @Mapping(
            target = "meetingsCountPlan",
            expression = "java( entity.getMeetingsCountPlan() )"
    )
    @Mapping(
            target = "meetingsCountFact",
            expression = "java( entity.getMeetingsCompletedCount() )"
    )
    @Mapping(
            target = "ntiMarkets",
            expression = "java( entity.getNtiMarkets().stream().map(NTIMarket::getName).toList() )")
    @Mapping(
            target = "readinessLevel",
            expression = "java( entity.getReadinessLevel().getValue() )")
    TeamCardReportRecordDto mapToReportDto(TeamCard entity);
}

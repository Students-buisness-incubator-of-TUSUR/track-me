package net.trackme.meetingservice.services.integration.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamCardDto {
    private UUID id;
    private String meetingRoomLink;
    private List<StreamDto> streams;
    private String username;
    private String name;
}
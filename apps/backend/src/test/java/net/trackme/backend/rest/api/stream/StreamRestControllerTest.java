package net.trackme.backend.rest.api.stream;

import net.trackme.backend.BaseApplicationTest;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.repos.NtiMarketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.time.Year;
import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WithMockUser(
        username = BaseApplicationTest.USER,
        roles = "SUPER_ADMIN")
class StreamRestControllerTest extends BaseApplicationTest {

    @Autowired
    private NtiMarketRepository ntiMarketRepository;

    @Autowired
    private ResourceLoader resourceLoader;


    @Test
    void findActiveStreams() throws Exception {
        mockMvc.perform(get("/api/v1/streams/active"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void getStreams_withFilters() throws Exception {
        streamRepository.save(Stream.builder()
                .name("stream 2")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .build());

        mockMvc.perform(post("/api/v1/streams")
                        .contentType("application/json")
                        .with(csrf())
                        .content("""
                                {
                                  "filters": [
                                    {
                                      "fieldName": "name",
                                      "type": "EQ",
                                      "value": "stream 1"
                                    }
                                  ]
                                }"""))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("stream 1"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void getNTIMarkets() throws Exception {
        mockMvc.perform(get("/api/v1/streams/nti-markets"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(15));
    }

    @Test
    void getStreams() throws Exception {
        streamRepository.save(Stream.builder()
                .name("stream 2")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .build());
        streamRepository.save(Stream.builder()
                .name("stream 3")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .build());

        mockMvc.perform(post("/api/v1/streams")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "filters": []
                                }"""))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("stream 1"))
                .andExpect(jsonPath("$.content[1].name").value("stream 2"))
                .andExpect(jsonPath("$.content[2].name").value("stream 3"))
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    void getStreams_withFilters_ntiMarket() throws Exception {
        var ntiMarket = ntiMarketRepository.findAll().getFirst();

        streamRepository.save(Stream.builder()
                .name("stream 2")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .ntiMarkets(Set.of(ntiMarket))
                .build());

        mockMvc.perform(post("/api/v1/streams")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "filters": [
                                    {
                                      "fieldName": "ntiMarkets.name",
                                      "type": "EQ",
                                      "value": "%s"
                                    },
                                    {
                                      "fieldName": "name",
                                      "type": "EQ",
                                      "value": "stream 2"
                                    }
                                  ]
                                }""".formatted(ntiMarket.getName())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("stream 2"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void getStreams_withFilters_withLikeName() throws Exception {
        mockMvc.perform(post("/api/v1/streams")
                        .contentType("application/json")
                        .with(csrf())
                        .content("""
                                {
                                  "filters": [
                                    {
                                      "fieldName": "name",
                                      "type": "LIKE",
                                      "value": "stream"
                                    }
                                  ]
                                }"""))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("stream 1"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void getStreams_withFilters_withNotExistingField() throws Exception {
        mockMvc.perform(post("/api/v1/streams")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "filters": [
                                    {
                                      "fieldName": "notExistingField",
                                      "type": "EQ",
                                      "value": "stream 1"
                                    }
                                  ]
                                }"""))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStreams_withFilters_withInvalidFilterField() throws Exception {
        mockMvc.perform(post("/api/v1/streams")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "filters": [
                                    {
                                      "fielqwqwdName": "name",
                                      "operationTweweype": "EQ",
                                      "value": "stream 1"
                                    }
                                  ]
                                }"""))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void addImage_success() throws Exception {
        var file = new MockMultipartFile("image", "image".getBytes());

        var uuid = streamRepository.findAll().getFirst().getId();
        mockMvc.perform(multipart("/api/v1/streams/%s/image".formatted(uuid))
                        .file("file", file.getBytes())
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void addImage_emptyFile() throws Exception {
        var uuid = streamRepository.findAll().getFirst().getId();
        mockMvc.perform(multipart("/api/v1/streams/%s/image".formatted(uuid))
                        .file("file", new byte[0])
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void addImage_largeFileSize() throws Exception {
        var uuid = streamRepository.findAll().getFirst().getId();
        mockMvc.perform(multipart("/api/v1/streams/%s/image".formatted(uuid))
                        .file("file", new byte[100*1024*1024 + 1])
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStreamImage_success() throws Exception {
        var stream = streamRepository.findAll().getFirst();
        stream.setImageBytes(resourceLoader.getResource("classpath:img/stream-image.png")
                .getInputStream()
                .readAllBytes());
        streamRepository.save(stream);

        mockMvc.perform(get("/api/v1/streams/%s/image".formatted(stream.getId())).with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().bytes(stream.getImageBytes()));
    }

    @Test
    void getStreamImage_notFound() throws Exception {
        mockMvc.perform(
                        get("/api/v1/streams/%s/image".formatted("00000000-0000-0000-0000-000000000000")))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void getStream_withFilters_byTeamCardReadinessLevel() throws Exception {
        var stream = streamRepository.findAll().getFirst();

        var uuid = ntiMarketRepository.findAll().getFirst().getId();
        mockMvc.perform(post("/api/v1/team-card")
                        .with(csrf())
                        .param("streamId", stream.getId().toString())
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "team card 1",
                                  "readinessLevel": "0-2",
                                  "description": "description",
                                  "meetingRoomLink": "https://test.link",
                                  "status": "OK",
                                  "ntiMarketIds": ["%s"]
                                }
                                """.formatted(uuid)))
                .andDo(print())
                .andExpect(status().isOk());


        mockMvc.perform(post("/api/v1/streams")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "filters": [
                                    {
                                      "fieldName": "teamCards.readinessLevel",
                                      "type": "EQ",
                                      "value": "0-2"
                                    }
                                  ]
                                }"""))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("stream 1"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void getStreams_withFilters_byYear() throws Exception {
        var startDate = Year.now();

        mockMvc.perform(post("/api/v1/streams")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "filters": [
                                    {
                                      "fieldName": "year",
                                      "type": "EQ",
                                      "value": "%s"
                                    }
                                  ]
                                }""".formatted(startDate.getValue())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("stream 1"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void getStreams_withFilters_byYears() throws Exception {
        var startDate = Year.now();

        mockMvc.perform(post("/api/v1/streams")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "filters": [
                                    {
                                      "fieldName": "year",
                                      "type": "EQ",
                                      "values": [
                                        "%s", "%s"
                                      ]
                                    }
                                  ]
                                }""".formatted(startDate.getValue(), startDate.plusYears(1).getValue())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("stream 1"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }
}
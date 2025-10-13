package net.trackme.backend.rest.api.admin;

import net.trackme.backend.BaseApplicationTest;
import net.trackme.backend.domain.NTIMarket;
import net.trackme.backend.domain.Stream;
import net.trackme.backend.repos.NtiMarketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser(value = BaseApplicationTest.USER, roles = "ADMIN")
class StreamAdminRestControllerImplTest extends BaseApplicationTest {

    @Autowired
    private NtiMarketRepository ntiMarketRepository;

    @Test
    void createStream_success() throws Exception {
        var trackStartDate = LocalDate.now().plusDays(1);
        var startDate = LocalDate.now();
        var endDate = startDate.plusYears(1);

        var ntiMarkets = ntiMarketRepository.findAll().stream()
                .map(NTIMarket::getId)
                .map(UUID::toString)
                .toList();

        mockMvc.perform(post("/api/v1/admin/stream")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test name",
                                  "startDate": "%s",
                                  "endDate": "%s",
                                  "ntiMarketIds": ["%s"],
                                  "description": "Test description",
                                  "trackStartDate": "%s"
                                }
                                """.formatted(startDate,
                                endDate,
                                String.join("\", \"", ntiMarkets),
                                trackStartDate)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name", is("Test name")))
                .andExpect(jsonPath("$.startDate", is(startDate.toString())))
                .andExpect(jsonPath("$.endDate", is(endDate.toString())))
                .andExpect(jsonPath("$.description", is("Test description")))
                .andExpect(jsonPath("$.trackStartDate", is(trackStartDate.toString())));
    }

    @Test
    void createStream_validationError() throws Exception {
        mockMvc.perform(post("/api/v1/admin/stream")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStream_success() throws Exception {
        var stream = streamRepository.findAll().getFirst();
        var trackStartDate = LocalDate.now().plusDays(1);
        var startDate = LocalDate.now();
        var endDate = startDate.plusYears(1);

        var ntiMarkets = ntiMarketRepository.findAll().stream()
                .map(NTIMarket::getId)
                .map(UUID::toString)
                .toList();

        mockMvc.perform(patch("/api/v1/admin/stream/%s".formatted(stream.getId().toString()))
                        .with(csrf())
                        .param("streamId", stream.getId().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "name": "Test name",
                                  "endDate": "%s",
                                  "trackStartDate": "%s",
                                  "ntiMarketIds": ["%s"]
                                }
                                """.formatted(endDate,
                            trackStartDate,
                            String.join("\", \"", ntiMarkets))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Test name")))
                .andExpect(jsonPath("$.endDate", is(endDate.toString())))
                .andExpect(jsonPath("$.trackStartDate", is(trackStartDate.toString())));
    }

    @Test
    @WithMockUser(value = BaseApplicationTest.USER, roles = "SUPER_ADMIN")
    void deleteStream_success() throws Exception {
        var stream = streamRepository.findAll().getFirst();

        mockMvc.perform(delete("/api/v1/admin/stream/%s".formatted(stream.getId().toString()))
                        .with(csrf())
                        .param("streamId", stream.getId().toString()))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    void findAllStreams_success() throws Exception {
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

        mockMvc.perform(post("/api/v1/admin/streams")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filters": []
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("stream 1"))
                .andExpect(jsonPath("$.content[1].name").value("stream 2"))
                .andExpect(jsonPath("$.content[2].name").value("stream 3"))
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    void getByIdStream_success() throws Exception {
        var ntiMarket = ntiMarketRepository.findAll().getFirst();
        var startDate = LocalDate.now();
        var endDate = startDate.plusYears(1);

        var stream = Stream.builder()
                .name("Test name")
                .startDate(startDate)
                .endDate(endDate)
                .ntiMarkets(Set.of(ntiMarket))
                .build();
        streamRepository.save(stream);

        mockMvc.perform(get("/api/v1/admin/stream/%s".formatted(stream.getId().toString()))
                        .with(csrf())
                        .param("streamId", stream.getId().toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(stream.getId().toString())))
                .andExpect(jsonPath("$.name", is(stream.getName())))
                .andExpect(jsonPath("$.startDate", is(stream.getStartDate().toString())))
                .andExpect(jsonPath("$.endDate", is(stream.getEndDate().toString())));
    }

    @Test
    void getByIdStream_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stream/%s".formatted("00000000-0000-0000-0000-000000000000"))
                        .with(csrf())
                        .param("streamId", "00000000-0000-0000-0000-000000000000"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}
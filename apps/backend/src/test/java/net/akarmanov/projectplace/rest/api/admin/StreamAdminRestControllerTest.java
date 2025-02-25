package net.akarmanov.projectplace.rest.api.admin;

import net.akarmanov.projectplace.BaseApplicationTest;
import net.akarmanov.projectplace.domain.Stream;
import net.akarmanov.projectplace.repos.NtiMarketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.util.Set;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser(username = BaseApplicationTest.USERNAME,
              roles = "SUPER_ADMIN")
class StreamAdminRestControllerTest extends BaseApplicationTest {

  @Autowired
  private NtiMarketRepository ntiMarketRepository;

  @Test
  void createStream_success() throws Exception {
    var ntiMarket = ntiMarketRepository.findAll().get(0);

    mockMvc.perform(post("/api/v1/admin/stream")
            .contentType(APPLICATION_JSON)
            .content("""
                {
                  "name": "createStream_success",
                  "startDate": "%s",
                  "endDate": "%s",
                  "description": "description",
                  "ntiMarketIds": ["%s"]
                }
                """.formatted(LocalDate.now(), LocalDate.now().plusDays(1), ntiMarket.getId())))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("createStream_success"));

    mockMvc.perform(post("/api/v1/admin/stream")
            .contentType(APPLICATION_JSON)
            .content("""
                {
                  "name": "createStream_success_2",
                  "startDate": "%s",
                  "endDate": "%s",
                  "description": "description",
                  "ntiMarketIds": ["%s"]
                }
                """.formatted(LocalDate.now(), LocalDate.now().plusDays(1), ntiMarket.getId())))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("createStream_success_2"));

    mockMvc.perform(post("/api/v1/admin/streams")
            .contentType(APPLICATION_JSON)
            .content("""
                {
                  "filters": [
                    {
                      "fieldName": "name",
                      "type": "LIKE",
                      "value": "createStream_success"
                    }
                  ]
                }
                """))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("createStream_success"))
        .andExpect(jsonPath("$.content[1].name").value("createStream_success_2"))
        .andExpect(jsonPath("$.page.totalElements").value(2));
  }

  @Test
  void updateStream_success() throws Exception {
    var ntiMarket = ntiMarketRepository.findAll().get(0);

    var stream = streamRepository.save(Stream.builder()
        .name("updateStream_success")
        .startDate(LocalDate.now())
        .endDate(LocalDate.now().plusDays(1))
        .ntiMarkets(Set.of(ntiMarket))
        .build());

    mockMvc.perform(put("/api/v1/admin/stream/{streamId}", stream.getId())
            .contentType(APPLICATION_JSON)
            .content("""
                {
                  "name": "updateStream_success_updated",
                  "startDate": "%s",
                  "endDate": "%s",
                  "description": "description",
                  "ntiMarketIds": ["%s"]
                }
                """.formatted(LocalDate.now(), LocalDate.now().plusDays(1), ntiMarket.getId())))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("updateStream_success_updated"));
  }

  @Test
  void deleteStream_success() throws Exception {
    var ntiMarket = ntiMarketRepository.findAll().get(0);

    var stream = streamRepository.save(Stream.builder()
        .name("deleteStream_success")
        .startDate(LocalDate.now())
        .endDate(LocalDate.now().plusDays(1))
        .ntiMarkets(Set.of(ntiMarket))
        .build());

    mockMvc.perform(post("/api/v1/admin/streams")
            .contentType(APPLICATION_JSON)
            .content("""
                {
                  "filters": [
                    {
                      "fieldName": "name",
                      "type": "LIKE",
                      "value": "deleteStream_success"
                    }
                  ]
                }
                """))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("deleteStream_success"))
        .andExpect(jsonPath("$.page.totalElements").value(1));

    mockMvc.perform(delete("/api/v1/admin/stream/{streamId}", stream.getId()))
        .andDo(print())
        .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/v1/admin/streams")
            .contentType(APPLICATION_JSON)
            .content("""
                {
                  "filters": [
                    {
                      "fieldName": "name",
                      "type": "LIKE",
                      "value": "deleteStream_success"
                    }
                  ]
                }
                """))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements").value(0));
  }

  @Test
  void findAllStreams_withFilters() throws Exception {
    var ntiMarket = ntiMarketRepository.findAll().get(0);

    streamRepository.save(Stream.builder()
        .name("stream 2")
        .startDate(LocalDate.now())
        .endDate(LocalDate.now().plusDays(1))
        .ntiMarkets(Set.of(ntiMarket))
        .build());

    mockMvc.perform(post("/api/v1/admin/streams")
            .contentType(APPLICATION_JSON)
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
            .contentType(APPLICATION_JSON)
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
  void create_and_get_streams() throws Exception {
    var ntiMarket = ntiMarketRepository.findAll().get(0);

    mockMvc.perform(post("/api/v1/admin/stream")
            .contentType(APPLICATION_JSON)
            .content("""
                {
                  "name": "create_and_get_streams",
                  "startDate": "%s",
                  "endDate": "%s",
                  "description": "description",
                  "ntiMarketIds": ["%s"]
                }
                """.formatted(LocalDate.now(), LocalDate.now().plusDays(1), ntiMarket.getId())))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("create_and_get_streams"));

    mockMvc.perform(post("/api/v1/streams")
            .contentType(APPLICATION_JSON)
            .content("""
                {
                  "filters": [
                    {
                      "fieldName": "name",
                      "type": "LIKE",
                      "value": "create_and_get_streams"
                    }
                  ]
                }
                """))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("create_and_get_streams"))
        .andExpect(jsonPath("$.page.totalElements").value(1));
  }

}
package net.akarmanov.projectplace.rest.api.stream;

import net.akarmanov.projectplace.BaseApplicationTest;
import net.akarmanov.projectplace.domain.ReadinessLevel;
import net.akarmanov.projectplace.domain.Stream;
import net.akarmanov.projectplace.repos.NtiMarketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WithMockUser(username = BaseApplicationTest.USERNAME,
              roles = "SUPER_ADMIN")
class StreamRestControllerTest extends BaseApplicationTest {

  @Autowired
  private NtiMarketRepository ntiMarketRepository;

  @Autowired
  private ResourceLoader resourceLoader;


  @Test
  void getCurrentStream() throws Exception {
    mockMvc.perform(get("/api/v1/streams/current"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("stream 1"));
  }

  @Test
  void getStreams_withFilters() throws Exception {
    streamRepository.save(Stream.builder()
        .name("stream 2")
        .startDate(LocalDate.now())
        .endDate(LocalDate.now().plusDays(1))
        .readinessLevel(ReadinessLevel.LEVEL_1)
        .build());

    mockMvc.perform(post("/api/v1/streams")
            .contentType("application/json")
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
    mockMvc.perform(post("/api/v1/streams")
            .contentType("application/json")
            .content("""
                {
                  "filters": []
                }"""))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("stream 1"))
        .andExpect(jsonPath("$.page.totalElements").value(1));
  }

  @Test
  void getStreams_withFilters_ntiMarket() throws Exception {
    var ntiMarket = ntiMarketRepository.findAll().get(0);

    streamRepository.save(Stream.builder()
        .name("stream 2")
        .startDate(LocalDate.now())
        .endDate(LocalDate.now().plusDays(1))
        .readinessLevel(ReadinessLevel.LEVEL_1)
        .ntiMarkets(Set.of(ntiMarket))
        .build());

    mockMvc.perform(post("/api/v1/streams")
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
                    },
                    {
                      "fieldName": "readinessLevel",
                      "type": "EQ",
                      "value": "0-2"
                    }
                  ]
                }""".formatted(ntiMarket.getName())))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("stream 2"))
        .andExpect(jsonPath("$.page.totalElements").value(1));
  }

  @Test
  void getStreams_withFilters_withIInvalidReadinessLevel() throws Exception {
    mockMvc.perform(post("/api/v1/streams")
            .contentType("application/json")
            .content("""
                {
                  "filters": [
                    {
                      "fieldName": "readinessLevel",
                      "type": "EQ",
                      "value": "0-2-3"
                    }
                  ]
                }"""))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty());
  }

  @Test
  void getStreams_withFilters_withLikeName() throws Exception {
    mockMvc.perform(post("/api/v1/streams")
            .contentType("application/json")
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

    var uuid = streamRepository.findAll().get(0).getId();
    mockMvc.perform(multipart("/api/v1/streams/%s/image".formatted(uuid))
            .file("file", file.getBytes()))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Test
  void addImage_emptyFile() throws Exception {
    var uuid = streamRepository.findAll().get(0).getId();
    mockMvc.perform(multipart("/api/v1/streams/%s/image".formatted(uuid))
            .file("file", new byte[0]))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  void getStreamImage_success() throws Exception {
    var stream = streamRepository.findAll().get(0);
    stream.setImageBytes(resourceLoader.getResource("classpath:img/stream-image.jpg")
        .getInputStream()
        .readAllBytes());
    streamRepository.save(stream);

    mockMvc.perform(get("/api/v1/streams/%s/image".formatted(stream.getId())))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().bytes(stream.getImageBytes()));
  }

  @Test
  void getStreamImage_notFound() throws Exception {
    mockMvc.perform(get("/api/v1/streams/%s/image".formatted("00000000-0000-0000-0000-000000000000")))
        .andDo(print())
        .andExpect(status().isNotFound());
  }
}
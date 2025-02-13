package net.akarmanov.projectplace.rest.api.user;

import net.akarmanov.projectplace.BaseApplicationTest;
import net.akarmanov.projectplace.domain.UserPhoto;
import net.akarmanov.projectplace.repos.UserPhotoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser(BaseApplicationTest.USERNAME)
class UserPhotoRestControllerImplTest extends BaseApplicationTest {

  @Autowired
  private UserPhotoRepository userPhotoRepository;

  @Value("classpath:test-image.jpg")
  private Resource testImage;


  @AfterEach
  void tearDown() {
    userPhotoRepository.deleteAll();
  }

  @Test
  void addPhoto_success() throws Exception {
    mockMvc.perform(multipart("/api/v1/users/{userId}/photo", user.getId())
            .file("file", testImage.getContentAsByteArray())
            .contentType(MediaType.MULTIPART_FORM_DATA))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Test
  void getPhoto_success() throws Exception {

    userPhotoRepository.save(UserPhoto.builder()
        .photo(testImage.getInputStream().readAllBytes())
        .user(user)
        .fileName("test-image.jpg")
        .build());

    mockMvc.perform(get("/api/v1/users/{userId}/photo", user.getId()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
        .andExpect(content().bytes(testImage.getInputStream().readAllBytes()));
  }

  @Test
  void deletePhoto_success() throws Exception {
    userPhotoRepository.save(UserPhoto.builder()
        .photo(testImage.getInputStream().readAllBytes())
        .user(user)
        .fileName("test-image.jpg")
        .build());

    mockMvc.perform(delete("/api/v1/users/{userId}/photo", user.getId()))
        .andDo(print())
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/v1/users/{userId}/photo", user.getId()))
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(content().string(containsString(user.getId().toString())));
  }

  @Test
  void getPhoto_notFound() throws Exception {
    mockMvc.perform(get("/api/v1/users/{userId}/photo", user.getId()))
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(content().string(containsString(user.getId().toString())));
  }

  @Test
  void deletePhoto_notFound() throws Exception {
    mockMvc.perform(delete("/api/v1/users/{userId}/photo", user.getId()))
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(content().string(containsString(user.getId().toString())));
  }
}
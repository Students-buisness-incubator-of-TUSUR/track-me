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
import org.springframework.mock.web.MockMultipartFile;
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

  @Value("classpath:test-image.png")
  private Resource testImage;


  @AfterEach
  void tearDown() {
    userPhotoRepository.deleteAll();
  }

  @Test
  void addPhoto_success() throws Exception {
    var mockImage = new MockMultipartFile("file", "test-image.png", MediaType.IMAGE_PNG_VALUE, "test-image".getBytes());

    mockMvc.perform(multipart("/api/v1/users/{telegramId}/photo", user.getTelegramId())
            .file(mockImage)
            .contentType(MediaType.MULTIPART_FORM_DATA))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Test
  void getPhoto_success() throws Exception {

    userPhotoRepository.save(UserPhoto.builder()
        .photo(testImage.getInputStream().readAllBytes())
        .user(user)
        .fileName("test-image.png")
        .build());

    mockMvc.perform(get("/api/v1/users/{telegramId}/photo", user.getTelegramId()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG))
        .andExpect(content().bytes(testImage.getInputStream().readAllBytes()));
  }

  @Test
  void deletePhoto_success() throws Exception {
    userPhotoRepository.save(UserPhoto.builder()
        .photo(testImage.getInputStream().readAllBytes())
        .user(user)
        .fileName("test-image.png")
        .build());

    mockMvc.perform(delete("/api/v1/users/{telegramId}/photo", user.getTelegramId()))
        .andDo(print())
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/v1/users/{telegramId}/photo", user.getTelegramId()))
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(content().string(containsString(user.getTelegramId())));
  }

  @Test
  void getPhoto_notFound() throws Exception {
    mockMvc.perform(get("/api/v1/users/{telegramId}/photo", user.getTelegramId()))
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(content().string(containsString(user.getTelegramId())));
  }

  @Test
  void deletePhoto_notFound() throws Exception {
    mockMvc.perform(delete("/api/v1/users/{telegramId}/photo", user.getTelegramId()))
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(content().string(containsString(user.getTelegramId())));
  }
}
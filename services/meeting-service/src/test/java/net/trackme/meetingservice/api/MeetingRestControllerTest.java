package net.trackme.meetingservice.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.trackme.meetingservice.AbstractIntegrationTest;
import net.trackme.meetingservice.dao.MeetingRepository;
import net.trackme.meetingservice.entities.Meeting;
import net.trackme.meetingservice.entities.MeetingStatus;
import net.trackme.meetingservice.entities.TeamStatus;
import net.trackme.meetingservice.services.MeetingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser(value = "superadmin", roles = {"SUPER_ADMIN"})
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc(print = MockMvcPrint.DEFAULT, printOnlyOnFailure = false)
@ActiveProfiles("test")
class MeetingRestControllerTest extends AbstractIntegrationTest {

    public static final UUID TEAM_CARD_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    @WithMockUser(value = "superadmin", roles = {"SUPER_ADMIN"})
    void setUp() {

        meetingRepository.save(Meeting.builder()
                .teamCardId(TEAM_CARD_ID)
                .link("https://example.com/meeting")
                .number("12345")
                .status(MeetingStatus.SCHEDULED)
                .startDate(OffsetDateTime.now().plusDays(1))
                .teamStatus(TeamStatus.OK)
                .tasksCurrentMeeting("tasksCurrentMeeting")
                .tasksNextMeeting("tasksNextMeeting")
                .build());

        meetingRepository.save(Meeting.builder()
                .teamCardId(TEAM_CARD_ID)
                .status(MeetingStatus.SCHEDULED)
                .link("https://example.com/meeting")
                .number("12343")
                .startDate(OffsetDateTime.now().plusDays(1))
                .teamStatus(TeamStatus.OK)
                .tasksCurrentMeeting("tasksCurrentMeeting")
                .tasksNextMeeting("tasksNextMeeting")
                .build());
    }

    @AfterEach
    void tearDown() {
        meetingRepository.deleteAll();
    }

    @Test
    @WithMockUser(value = "superadmin", roles = {"SUPER_ADMIN"})
    void createMeeting_success() throws Exception {

        var meetingCreateDto = MeetingCreateDto.builder()
                .link("https://example.com/meeting")
                .number("12345")
                .startDate(OffsetDateTime.now().plusDays(1))
                .build();

        mockMvc.perform(post("/api/v1/create-meeting")
                        .param("teamCardId", TEAM_CARD_ID.toString())
                        .contentType("application/json")
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(meetingCreateDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.link").value(meetingCreateDto.link()))
                .andExpect(jsonPath("$.number").value(meetingCreateDto.number()))
                .andExpect(jsonPath("$.teamCardId").value(TEAM_CARD_ID.toString()));
    }

    @Test
    void getMeetings_success() throws Exception {
        mockMvc.perform(get("/api/v1/meetings")
                        .with(csrf())
                        .param("teamCardId", TEAM_CARD_ID.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(value = "superadmin", roles = {"SUPER_ADMIN"})
    void getMeetings_success_superAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/meetings")
                        .with(csrf())
                        .param("teamCardId", TEAM_CARD_ID.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void updateMeeting_success() throws Exception {
        var meetingUpdateDto = MeetingUpdateDto.builder()
                .link("https://example.com/meeting")
                .number("12345")
                .teamStatus(TeamStatus.MANY_ISSUES)
                .build();

        var meetingId = meetingRepository.findAll().getFirst().getId().toString();
        mockMvc.perform(patch("/api/v1/update-meeting/" + meetingId)
                        .param("teamCardId", TEAM_CARD_ID.toString())
                        .contentType("application/json")
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(meetingUpdateDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.id").value(meetingId))
                .andExpect(jsonPath("$.link").value("https://example.com/meeting"))
                .andExpect(jsonPath("$.number").value("12345"));
    }

    @Test
    @WithMockUser(value = "superadmin", roles = {"SUPER_ADMIN"})
    void updateMeeting_bySuperAdmin_success() throws Exception {
        var meetingUpdateDto = MeetingUpdateDto.builder()
                .link("https://example.com/meeting")
                .number("123456")
                .teamStatus(TeamStatus.OK)
                .build();

        var meetingId = meetingRepository.findAll().getFirst().getId().toString();
        mockMvc.perform(patch("/api/v1/update-meeting/" + meetingId)
                        .param("teamCardId", TEAM_CARD_ID.toString())
                        .contentType("application/json")
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(meetingUpdateDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.id").value(meetingId))
                .andExpect(jsonPath("$.link").value("https://example.com/meeting"))
                .andExpect(jsonPath("$.number").value("123456"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/png", "image/jpeg"})
    void testAddImage_success(String contentType) throws Exception {
        var meetingId = meetingRepository.findAll().getFirst().getId().toString();
        var mockMultipartFile = new MockMultipartFile(
                "file", "test.png", contentType, "test".getBytes());
        mockMvc.perform(multipart("/api/v1/image/" + meetingId)
                        .file(mockMultipartFile)
                        .with(csrf())
                        .contentType("multipart/form-data"))
                .andDo(print())
                .andExpect(status().isOk());

        Assertions.assertNotNull(
                meetingRepository.findById(UUID.fromString(meetingId)).get().getImageBytes());
    }

    @Test
    void testAddImage_invalidContentType() throws Exception {
        var meetingId = meetingRepository.findAll().getFirst().getId().toString();
        var mockMultipartFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", "test".getBytes());
        mockMvc.perform(multipart("/api/v1/image/" + meetingId)
                        .file(mockMultipartFile)
                        .with(csrf())
                        .contentType("multipart/form-data"))
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void testAddImage_largeFile() throws Exception {
        var meetingId = meetingRepository.findAll().getFirst().getId().toString();
        var mockMultipartFile = new MockMultipartFile(
                "file", "test.png", "image/png", new byte[MeetingService.MAX_FILE_SIZE + 1]);
        mockMvc.perform(multipart("/api/v1/image/" + meetingId)
                        .file(mockMultipartFile)
                        .with(csrf())
                        .contentType("multipart/form-data"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAddImage_emptyFile() throws Exception {
        var meetingId = meetingRepository.findAll().getFirst().getId().toString();
        var mockMultipartFile = new MockMultipartFile("file", "test.png", "image/png", new byte[0]);
        mockMvc.perform(multipart("/api/v1/image/" + meetingId)
                        .file(mockMultipartFile)
                        .with(csrf())
                        .contentType("multipart/form-data"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAddImage_invalidFileExtension() throws Exception {
        var meetingId = meetingRepository.findAll().getFirst().getId().toString();
        var mockMultipartFile = new MockMultipartFile(
                "file", "test.txt", "image/png", "test".getBytes());
        mockMvc.perform(multipart("/api/v1/image/" + meetingId)
                        .file(mockMultipartFile)
                        .with(csrf())
                        .contentType("multipart/form-data"))
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void testGetImage_success() throws Exception {
        var meetingId = meetingRepository.findAll().getFirst().getId();
        meetingService.addMeetingImage(
                meetingId,
                new MockMultipartFile("file", "test.png", "image/png", "test".getBytes()));
        mockMvc.perform(get("/api/v1/image/" + meetingId).with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }

    @Test
    void testGetImage_notFound() throws Exception {
        var meetingId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/image/" + meetingId).with(csrf()))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteMeeting_success() throws Exception {
        var meetingId = meetingRepository.findAll().getFirst().getId().toString();
        mockMvc.perform(delete("/api/v1/delete-meeting/" + meetingId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());

        Assertions.assertFalse(meetingRepository.existsById(UUID.fromString(meetingId)));
    }
}
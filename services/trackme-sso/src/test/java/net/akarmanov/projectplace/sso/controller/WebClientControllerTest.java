package net.akarmanov.projectplace.sso.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(WebClientController.class)
@AutoConfigureMockMvc(addFilters = false)
class WebClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testIndex_ReturnsIndexView() throws Exception {
        mockMvc.perform(get("/client/registration"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }
}
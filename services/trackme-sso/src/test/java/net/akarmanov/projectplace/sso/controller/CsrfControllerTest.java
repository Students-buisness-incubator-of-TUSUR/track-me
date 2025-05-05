package net.akarmanov.projectplace.sso.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CsrfController.class)
@AutoConfigureMockMvc(addFilters = false)
class CsrfControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private CsrfToken csrfToken;

    @Test
    void shouldReturnCsrfTokenDetails() throws Exception {
        when(csrfToken.getParameterName()).thenReturn("csrfParameter");
        when(csrfToken.getToken()).thenReturn("csrfTokenValue");

        mockMvc.perform(get("/api/csrf")
                        .requestAttr(CsrfToken.class.getName(), csrfToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parameterName").value("csrfParameter"))
                .andExpect(jsonPath("$.token").value("csrfTokenValue"));
    }
}
package com.lectuaria.backend.controller.book;

import com.lectuaria.backend.dto.book.PlatformDTO;
import com.lectuaria.backend.service.book.IPlatformService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlatformControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IPlatformService platformService;

    @Test
    @WithMockUser
    void getAllPlatforms_returnsPlatformList() throws Exception {
        when(platformService.getAllPlatforms()).thenReturn(List.of(
                new PlatformDTO(1L, "Kindle"),
                new PlatformDTO(2L, "Kobo"),
                new PlatformDTO(3L, "Google Play Books")
        ));

        mockMvc.perform(get("/api/platforms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Kindle"));
    }

    @Test
    @WithMockUser
    void getAllPlatforms_returnsEmptyListWhenNoPlatforms() throws Exception {
        when(platformService.getAllPlatforms()).thenReturn(List.of());

        mockMvc.perform(get("/api/platforms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
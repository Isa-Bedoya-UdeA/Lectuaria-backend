package com.lectuaria.backend.service.book.impl;

import com.lectuaria.backend.dto.book.PlatformDTO;
import com.lectuaria.backend.model.book.Platform;
import com.lectuaria.backend.repository.book.PlatformRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlatformServiceImplTest {

    @Mock
    private PlatformRepository platformRepository;

    @InjectMocks
    private PlatformServiceImpl platformService;

    private Platform kindle;
    private Platform kobo;

    @BeforeEach
    void setUp() {
        kindle = new Platform();
        kindle.setName("Kindle");
        // Use reflection to set id since there's no setter for id
        setId(kindle, 1L);

        kobo = new Platform();
        kobo.setName("Kobo");
        setId(kobo, 2L);
    }

    private void setId(Platform platform, Long id) {
        try {
            var field = Platform.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(platform, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getAllPlatforms_returnsAllPlatforms() {
        when(platformRepository.findAll()).thenReturn(Arrays.asList(kindle, kobo));

        List<PlatformDTO> result = platformService.getAllPlatforms();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Kindle");
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getName()).isEqualTo("Kobo");
        assertThat(result.get(1).getId()).isEqualTo(2L);
        verify(platformRepository).findAll();
    }

    @Test
    void getAllPlatforms_returnsEmptyListWhenNoPlatforms() {
        when(platformRepository.findAll()).thenReturn(Collections.emptyList());

        List<PlatformDTO> result = platformService.getAllPlatforms();

        assertThat(result).isEmpty();
        verify(platformRepository).findAll();
    }

    @Test
    void getAllPlatforms_returnsSinglePlatform() {
        when(platformRepository.findAll()).thenReturn(List.of(kindle));

        List<PlatformDTO> result = platformService.getAllPlatforms();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Kindle");
    }
}
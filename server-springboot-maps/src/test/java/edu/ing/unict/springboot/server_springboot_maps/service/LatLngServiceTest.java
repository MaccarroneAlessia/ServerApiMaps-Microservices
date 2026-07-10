package edu.ing.unict.springboot.server_springboot_maps.service;

import edu.ing.unict.springboot.server_springboot_maps.model.LatLng;
import edu.ing.unict.springboot.server_springboot_maps.repository.LatLngRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LatLngServiceTest {

    @Mock
    private LatLngRepository latLngRepository;

    @InjectMocks
    private LatLngService latLngService;

    @Test
    void testFindOrCreateLatLng_WhenExists() {
        // Arrange
        Double lat = 37.5;
        Double lng = 15.0;
        LatLng existing = new LatLng(lat, lng, "Esistente");
        // Mock comportamento: trova la località
        when(latLngRepository.findByLatitudeAndLongitude(lat, lng)).thenReturn(Optional.of(existing));

        // Act
        LatLng result = latLngService.findOrCreateLatLng(lat, lng);

        // Assert
        assertNotNull(result);
        assertEquals("Esistente", result.getName());
        verify(latLngRepository, times(1)).findByLatitudeAndLongitude(lat, lng);
        verify(latLngRepository, never()).save(any(LatLng.class)); // Non deve salvare
    }

    @Test
    void testFindOrCreateLatLng_WhenNotExists() {
        // Arrange
        Double lat = 37.5;
        Double lng = 15.0;
        // Mock comportamento: non trova nulla
        when(latLngRepository.findByLatitudeAndLongitude(lat, lng)).thenReturn(Optional.empty());
        // Mock comportamento: salva il nuovo oggetto
        when(latLngRepository.save(any(LatLng.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        LatLng result = latLngService.findOrCreateLatLng(lat, lng);

        // Assert
        assertNotNull(result);
        assertTrue(result.getName().startsWith("Loc 37"));
        verify(latLngRepository, times(1)).findByLatitudeAndLongitude(lat, lng);
        verify(latLngRepository, times(1)).save(any(LatLng.class)); // Deve salvare il nuovo
    }
}

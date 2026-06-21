package net.trackme.backend.services.nti;

import net.trackme.backend.domain.NTIMarket;
import net.trackme.backend.repos.NtiMarketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = {NtiMarketServiceImpl.class})
class NtiMarketServiceImplTest {

    @MockitoBean
    private NtiMarketRepository ntiMarketRepository;

    @Autowired
    private NtiMarketServiceImpl ntiMarketService;

    public NtiMarketServiceImplTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should return list of NTIMarkets when IDs are valid")
    void shouldReturnNtiMarketsWhenIdsAreValid() {
        // Arrange
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        NTIMarket market1 = NTIMarket.builder().id(id1).build();
        NTIMarket market2 = NTIMarket.builder().id(id2).build();
        List<UUID> ids = List.of(id1, id2);
        when(ntiMarketRepository.findAllById(ids)).thenReturn(List.of(market1, market2));

        // Act
        List<NTIMarket> result = ntiMarketService.getNtiMarkets(ids);

        // Assert
        assertThat(result).containsExactlyInAnyOrder(market1, market2);
    }

    @Test
    @DisplayName("Should throw NtiMarketNotFoundException when no markets are found for the IDs")
    void shouldThrowExceptionWhenNoMarketsFound() {
        // Arrange
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<UUID> ids = List.of(id1, id2);
        when(ntiMarketRepository.findAllById(ids)).thenReturn(new ArrayList<>());

        // Act & Assert
        assertThatThrownBy(() -> ntiMarketService.getNtiMarkets(ids))
                .isInstanceOf(NtiMarketNotFoundException.class)
                .hasMessageContaining(ids.toString());
    }

    @Test
    @DisplayName("Should handle empty input list of IDs gracefully")
    void shouldHandleEmptyInputList() {
        // Arrange
        List<UUID> ids = new ArrayList<>();
        when(ntiMarketRepository.findAllById(ids)).thenReturn(new ArrayList<>());

        // Act & Assert
        assertThatThrownBy(() -> ntiMarketService.getNtiMarkets(ids))
                .isInstanceOf(NtiMarketNotFoundException.class)
                .hasMessageContaining(ids.toString());
    }
}
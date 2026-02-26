package com.portfolio.service;

import com.portfolio.dto.AssetDto;
import com.portfolio.dto.PortfolioSummary;
import com.portfolio.model.Asset;
import com.portfolio.repository.AssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private PortfolioService portfolioService;

    private Asset appleAsset;
    private Asset bitcoinAsset;

    @BeforeEach
    void setUp() {
        // Apple: kupiono za 100, teraz 150 → ROI = 50%
        appleAsset = Asset.builder()
                .id(1L)
                .name("Apple")
                .quantity(new BigDecimal("10"))
                .purchasePrice(new BigDecimal("100"))
                .currentPrice(new BigDecimal("150"))
                .build();

        // Bitcoin: kupiono za 40000, teraz 30000 → ROI = -25%
        bitcoinAsset = Asset.builder()
                .id(2L)
                .name("Bitcoin")
                .quantity(new BigDecimal("1"))
                .purchasePrice(new BigDecimal("40000"))
                .currentPrice(new BigDecimal("30000"))
                .build();
    }

    // -----------------------------------------------
    // Testy obliczenia ROI
    // -----------------------------------------------

    @Test
    @DisplayName("ROI powinno wynosić 50% gdy cena wzrosła z 100 do 150")
    void shouldCalculatePositiveROI() {
        BigDecimal roi = portfolioService.calculateROI(
                new BigDecimal("10"),
                new BigDecimal("100"),
                new BigDecimal("150")
        );

        assertThat(roi).isEqualByComparingTo(new BigDecimal("50.0000"));
    }

    @Test
    @DisplayName("ROI powinno wynosić -25% gdy cena spadła z 40000 do 30000")
    void shouldCalculateNegativeROI() {
        BigDecimal roi = portfolioService.calculateROI(
                new BigDecimal("1"),
                new BigDecimal("40000"),
                new BigDecimal("30000")
        );

        assertThat(roi).isEqualByComparingTo(new BigDecimal("-25.0000"));
    }

    @Test
    @DisplayName("ROI powinno wynosić 0 gdy investedValue = 0 (purchasePrice = 0)")
    void shouldReturnZeroROIWhenInvestedValueIsZero() {
        BigDecimal roi = portfolioService.calculateROI(
                new BigDecimal("10"),
                BigDecimal.ZERO,   // purchasePrice = 0 → investedValue = 0
                new BigDecimal("100")
        );

        assertThat(roi).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // -----------------------------------------------
    // Testy totalProfit
    // -----------------------------------------------

    @Test
    @DisplayName("totalProfit powinien być sumą zysku ze wszystkich assetów")
    void shouldCalculateTotalProfitCorrectly() {
        when(assetRepository.findAll()).thenReturn(List.of(appleAsset, bitcoinAsset));

        PortfolioSummary summary = portfolioService.getPortfolioSummary();

        // Apple: 10 * 150 - 10 * 100 = 1500 - 1000 = +500
        // Bitcoin: 1 * 30000 - 1 * 40000 = -10000
        // totalProfit = 500 + (-10000) = -9500
        assertThat(summary.getTotalProfit()).isEqualByComparingTo(new BigDecimal("-9500"));
    }

    @Test
    @DisplayName("totalCurrentValue powinno być sumą wartości rynkowych")
    void shouldCalculateTotalCurrentValue() {
        when(assetRepository.findAll()).thenReturn(List.of(appleAsset, bitcoinAsset));

        PortfolioSummary summary = portfolioService.getPortfolioSummary();

        // Apple: 10 * 150 = 1500
        // Bitcoin: 1 * 30000 = 30000
        // Total = 31500
        assertThat(summary.getTotalCurrentValue()).isEqualByComparingTo(new BigDecimal("31500"));
    }

    // -----------------------------------------------
    // Test largestAsset
    // -----------------------------------------------

    @Test
    @DisplayName("largestAssetName powinien wskazywać na asset o największej wartości rynkowej")
    void shouldReturnLargestAssetName() {
        when(assetRepository.findAll()).thenReturn(List.of(appleAsset, bitcoinAsset));

        PortfolioSummary summary = portfolioService.getPortfolioSummary();

        // Apple = 1500, Bitcoin = 30000 → largest = Bitcoin
        assertThat(summary.getLargestAssetName()).isEqualTo("Bitcoin");
    }

    // -----------------------------------------------
    // Pustry portfel
    // -----------------------------------------------

    @Test
    @DisplayName("Puste portfolio nie powinno rzucać wyjątku i zwracać zerowe wartości")
    void shouldHandleEmptyPortfolioGracefully() {
        when(assetRepository.findAll()).thenReturn(Collections.emptyList());

        PortfolioSummary summary = portfolioService.getPortfolioSummary();

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalCurrentValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getTotalInvestedValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getTotalProfit()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getAverageROI()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getLargestAssetName()).isNull();
    }

    @Test
    @DisplayName("portfolioShare powinien wynosić 100% gdy jest tylko jeden asset")
    void shouldCalculate100PercentShareForSingleAsset() {
        when(assetRepository.findAll()).thenReturn(List.of(appleAsset));

        List<AssetDto> assets = portfolioService.getAllAssets();

        assertThat(assets).hasSize(1);
        assertThat(assets.get(0).getPortfolioShare()).isEqualByComparingTo(new BigDecimal("100.0000"));
    }

    @Test
    @DisplayName("averageROI powinno być średnią ROI wszystkich assetów")
    void shouldCalculateAverageROI() {
        when(assetRepository.findAll()).thenReturn(List.of(appleAsset, bitcoinAsset));

        PortfolioSummary summary = portfolioService.getPortfolioSummary();

        // Apple ROI = 50%, Bitcoin ROI = -25%
        // averageROI = (50 + (-25)) / 2 = 12.5%
        assertThat(summary.getAverageROI()).isEqualByComparingTo(new BigDecimal("12.5000"));
    }
}

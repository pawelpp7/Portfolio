package com.portfolio.service;

import com.portfolio.dto.AssetDto;
import com.portfolio.dto.PortfolioSummary;
import com.portfolio.exception.AssetNotFoundException;
import com.portfolio.model.Asset;
import com.portfolio.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final AssetRepository assetRepository;

    // -----------------------------------------------
    // CRUD
    // -----------------------------------------------

    @Transactional
    public AssetDto addAsset(AssetDto dto) {
        Asset asset = toEntity(dto);
        Asset saved = assetRepository.save(asset);
        return toDto(saved, BigDecimal.ZERO); // portfolioShare obliczamy przy liście
    }

    @Transactional(readOnly = true)
    public List<AssetDto> getAllAssets() {
        List<Asset> assets = assetRepository.findAll();
        BigDecimal totalCurrentValue = calculateTotalCurrentValue(assets);
        return assets.stream()
                .map(a -> toDto(a, totalCurrentValue))
                .toList();
    }

    @Transactional
    public void deleteAsset(Long id) {
        if (!assetRepository.existsById(id)) {
            throw new AssetNotFoundException(id);
        }
        assetRepository.deleteById(id);
    }

    // -----------------------------------------------
    // Portfolio analytics
    // -----------------------------------------------

    @Transactional(readOnly = true)
    public PortfolioSummary getPortfolioSummary() {
        List<Asset> assets = assetRepository.findAll();

        if (assets.isEmpty()) {
            return PortfolioSummary.builder()
                    .totalCurrentValue(BigDecimal.ZERO)
                    .totalInvestedValue(BigDecimal.ZERO)
                    .totalProfit(BigDecimal.ZERO)
                    .averageROI(BigDecimal.ZERO)
                    .largestAssetName(null)
                    .build();
        }

        BigDecimal totalCurrentValue = calculateTotalCurrentValue(assets);
        BigDecimal totalInvestedValue = calculateTotalInvestedValue(assets);
        BigDecimal totalProfit = totalCurrentValue.subtract(totalInvestedValue);

        BigDecimal averageROI = assets.stream()
                .map(a -> calculateROI(a.getQuantity(), a.getPurchasePrice(), a.getCurrentPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(assets.size()), SCALE, ROUNDING);

        String largestAssetName = assets.stream()
                .max(Comparator.comparing(a -> calculateCurrentValue(a.getQuantity(), a.getCurrentPrice())))
                .map(Asset::getName)
                .orElse(null);

        return PortfolioSummary.builder()
                .totalCurrentValue(totalCurrentValue.setScale(SCALE, ROUNDING))
                .totalInvestedValue(totalInvestedValue.setScale(SCALE, ROUNDING))
                .totalProfit(totalProfit.setScale(SCALE, ROUNDING))
                .averageROI(averageROI)
                .largestAssetName(largestAssetName)
                .build();
    }

    @Transactional(readOnly = true)
    public AssetDto getTopAsset() {
        List<Asset> assets = assetRepository.findAll();
        BigDecimal totalCurrentValue = calculateTotalCurrentValue(assets);

        return assets.stream()
                .max(Comparator.comparing(a -> calculateROI(a.getQuantity(), a.getPurchasePrice(), a.getCurrentPrice())))
                .map(a -> toDto(a, totalCurrentValue))
                .orElseThrow(() -> new AssetNotFoundException(-1L));
    }

    // -----------------------------------------------
    // Obliczenia (public dla testów jednostkowych)
    // -----------------------------------------------

    public BigDecimal calculateCurrentValue(BigDecimal quantity, BigDecimal currentPrice) {
        return quantity.multiply(currentPrice).setScale(SCALE, ROUNDING);
    }

    public BigDecimal calculateInvestedValue(BigDecimal quantity, BigDecimal purchasePrice) {
        return quantity.multiply(purchasePrice).setScale(SCALE, ROUNDING);
    }

    /**
     * ROI = (currentValue - investedValue) / investedValue * 100
     * Jeśli investedValue == 0, zwraca BigDecimal.ZERO.
     */
    public BigDecimal calculateROI(BigDecimal quantity, BigDecimal purchasePrice, BigDecimal currentPrice) {
        BigDecimal investedValue = calculateInvestedValue(quantity, purchasePrice);
        if (investedValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal currentValue = calculateCurrentValue(quantity, currentPrice);
        return currentValue.subtract(investedValue)
                .divide(investedValue, MathContext.DECIMAL128)
                .multiply(BigDecimal.valueOf(100))
                .setScale(SCALE, ROUNDING);
    }

    // -----------------------------------------------
    // Helpers
    // -----------------------------------------------

    private BigDecimal calculateTotalCurrentValue(List<Asset> assets) {
        return assets.stream()
                .map(a -> calculateCurrentValue(a.getQuantity(), a.getCurrentPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalInvestedValue(List<Asset> assets) {
        return assets.stream()
                .map(a -> calculateInvestedValue(a.getQuantity(), a.getPurchasePrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private AssetDto toDto(Asset asset, BigDecimal totalCurrentValue) {
        BigDecimal currentValue = calculateCurrentValue(asset.getQuantity(), asset.getCurrentPrice());
        BigDecimal investedValue = calculateInvestedValue(asset.getQuantity(), asset.getPurchasePrice());
        BigDecimal roi = calculateROI(asset.getQuantity(), asset.getPurchasePrice(), asset.getCurrentPrice());

        BigDecimal portfolioShare = BigDecimal.ZERO;
        if (totalCurrentValue.compareTo(BigDecimal.ZERO) != 0) {
            portfolioShare = currentValue
                    .divide(totalCurrentValue, MathContext.DECIMAL128)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(SCALE, ROUNDING);
        }

        return AssetDto.builder()
                .id(asset.getId())
                .name(asset.getName())
                .quantity(asset.getQuantity())
                .purchasePrice(asset.getPurchasePrice())
                .currentPrice(asset.getCurrentPrice())
                .currentValue(currentValue)
                .investedValue(investedValue)
                .roi(roi)
                .portfolioShare(portfolioShare)
                .build();
    }

    private Asset toEntity(AssetDto dto) {
        return Asset.builder()
                .name(dto.getName())
                .quantity(dto.getQuantity())
                .purchasePrice(dto.getPurchasePrice())
                .currentPrice(dto.getCurrentPrice())
                .build();
    }
}

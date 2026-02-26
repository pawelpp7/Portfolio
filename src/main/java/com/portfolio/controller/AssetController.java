package com.portfolio.controller;

import com.portfolio.dto.AssetDto;
import com.portfolio.dto.PortfolioSummary;
import com.portfolio.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AssetController {

    private final PortfolioService portfolioService;

    // POST /assets - dodaj nowy asset
    @PostMapping("/assets")
    public ResponseEntity<AssetDto> addAsset(@Valid @RequestBody AssetDto dto) {
        AssetDto created = portfolioService.addAsset(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // GET /assets - lista wszystkich assetów z obliczonymi wartościami
    @GetMapping("/assets")
    public ResponseEntity<List<AssetDto>> getAllAssets() {
        return ResponseEntity.ok(portfolioService.getAllAssets());
    }

    // DELETE /assets/{id}
    @DeleteMapping("/assets/{id}")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long id) {
        portfolioService.deleteAsset(id);
        return ResponseEntity.noContent().build();
    }

    // GET /portfolio/summary - podsumowanie całego portfela
    @GetMapping("/portfolio/summary")
    public ResponseEntity<PortfolioSummary> getPortfolioSummary() {
        return ResponseEntity.ok(portfolioService.getPortfolioSummary());
    }

    // GET /portfolio/top - asset z najlepszym ROI
    @GetMapping("/portfolio/top")
    public ResponseEntity<AssetDto> getTopAsset() {
        return ResponseEntity.ok(portfolioService.getTopAsset());
    }
}

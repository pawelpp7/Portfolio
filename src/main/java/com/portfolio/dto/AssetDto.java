package com.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetDto {
    private Long id;

    @NotBlank(message = "Name cannot be blank")
    private String name;

    @NotNull @Positive
    private BigDecimal quantity;

    @NotNull @Positive
    private BigDecimal purchasePrice;

    @NotNull @Positive
    private BigDecimal currentPrice;

    private BigDecimal currentValue;
    private BigDecimal investedValue;
    private BigDecimal roi;
    private BigDecimal portfolioShare; // udział procentowy w portfelu
}

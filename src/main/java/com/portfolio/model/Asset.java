package com.portfolio.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name cannot be blank")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Quantity cannot be null")
    @Positive(message = "Quantity must be positive")
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    @NotNull(message = "Purchase price cannot be null")
    @Positive(message = "Purchase price must be positive")
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal purchasePrice;

    @NotNull(message = "Current price cannot be null")
    @Positive(message = "Current price must be positive")
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal currentPrice;
}

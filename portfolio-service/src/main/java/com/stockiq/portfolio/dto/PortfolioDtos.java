package com.stockiq.portfolio.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class PortfolioDtos {

    public record CreatePortfolioRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description
    ) {}

    public record UpdatePortfolioRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description
    ) {}

    public record PortfolioResponse(
        UUID id, String name, String description,
        List<HoldingResponse> holdings,
        BigDecimal totalValue, BigDecimal totalCost,
        BigDecimal unrealizedPL, BigDecimal unrealizedPLPercent,
        Instant createdAt, Instant updatedAt
    ) {}

    public record AddHoldingRequest(
        @NotBlank @Size(max = 20) String symbol,
        @NotBlank @Size(max = 200) String name,
        @NotNull @DecimalMin("0.0001") BigDecimal quantity,
        @NotNull @DecimalMin("0.0001") BigDecimal avgCostBasis,
        @NotNull @DecimalMin("0.0001") BigDecimal currentPrice,
        @NotBlank String sector,
        @NotBlank String assetType,
        @NotNull LocalDate purchaseDate
    ) {}

    public record UpdateHoldingRequest(
        @NotNull @DecimalMin("0.0001") BigDecimal quantity,
        @NotNull @DecimalMin("0.0001") BigDecimal avgCostBasis,
        @NotNull @DecimalMin("0.0001") BigDecimal currentPrice,
        @NotBlank String sector,
        @NotNull LocalDate purchaseDate
    ) {}

    public record HoldingResponse(
        UUID id, String symbol, String name,
        BigDecimal quantity, BigDecimal avgCostBasis, BigDecimal currentPrice,
        BigDecimal marketValue, BigDecimal unrealizedPL, BigDecimal unrealizedPLPercent,
        String sector, String assetType, LocalDate purchaseDate
    ) {}

    public record AllocationResponse(
        List<AllocationSlice> bySector,
        List<AllocationSlice> byAssetType,
        BigDecimal totalValue
    ) {}

    public record AllocationSlice(String label, BigDecimal value, BigDecimal percent) {}

    public record ExportRow(
        String symbol, String name, BigDecimal quantity,
        BigDecimal avgCostBasis, BigDecimal currentPrice,
        BigDecimal marketValue, BigDecimal unrealizedPL,
        BigDecimal unrealizedPLPercent, String sector,
        String assetType, LocalDate purchaseDate
    ) {}
}

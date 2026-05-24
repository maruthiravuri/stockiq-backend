package com.stockiq.portfolio.service;

import com.stockiq.portfolio.dto.PortfolioDtos.*;
import com.stockiq.portfolio.entity.Holding;
import com.stockiq.portfolio.entity.Portfolio;
import com.stockiq.portfolio.exception.ResourceNotFoundException;
import com.stockiq.portfolio.repository.HoldingRepository;
import com.stockiq.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;

    // ── Portfolio CRUD ────────────────────────────────────────────────────────

    @Transactional
    public PortfolioResponse createPortfolio(UUID userId, CreatePortfolioRequest req) {
        var portfolio = Portfolio.builder()
                .name(req.name())
                .description(req.description())
                .userId(userId)
                .build();
        portfolio = portfolioRepository.save(portfolio);
        log.info("Created portfolio '{}' for user {}", req.name(), userId);
        return toResponse(portfolio);
    }

    @Transactional(readOnly = true)
    public List<PortfolioResponse> getUserPortfolios(UUID userId) {
        return portfolioRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolio(UUID portfolioId, UUID userId) {
        var portfolio = portfolioRepository.findByIdAndUserIdWithHoldings(portfolioId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", portfolioId.toString()));
        return toResponse(portfolio);
    }

    @Transactional
    public PortfolioResponse updatePortfolio(UUID portfolioId, UUID userId, UpdatePortfolioRequest req) {
        var portfolio = portfolioRepository.findByIdAndUserIdWithHoldings(portfolioId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", portfolioId.toString()));
        portfolio.setName(req.name());
        portfolio.setDescription(req.description());
        return toResponse(portfolioRepository.save(portfolio));
    }

    @Transactional
    public void deletePortfolio(UUID portfolioId, UUID userId) {
        if (!portfolioRepository.existsByIdAndUserId(portfolioId, userId)) {
            throw new ResourceNotFoundException("Portfolio", portfolioId.toString());
        }
        portfolioRepository.deleteById(portfolioId);
    }

    // ── Holdings CRUD ─────────────────────────────────────────────────────────

    @Transactional
    public HoldingResponse addHolding(UUID portfolioId, UUID userId, AddHoldingRequest req) {
        var portfolio = portfolioRepository.findByIdAndUserIdWithHoldings(portfolioId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", portfolioId.toString()));

        var holding = Holding.builder()
                .portfolio(portfolio)
                .symbol(req.symbol().toUpperCase())
                .name(req.name())
                .quantity(req.quantity())
                .avgCostBasis(req.avgCostBasis())
                .currentPrice(req.currentPrice())
                .sector(req.sector())
                .assetType(req.assetType())
                .purchaseDate(req.purchaseDate())
                .build();

        holding = holdingRepository.save(holding);
        log.info("Added holding {} to portfolio {}", req.symbol(), portfolioId);
        return toHoldingResponse(holding);
    }

    @Transactional
    public HoldingResponse updateHolding(UUID portfolioId, UUID userId, UUID holdingId, UpdateHoldingRequest req) {
        portfolioRepository.findByIdAndUserIdWithHoldings(portfolioId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", portfolioId.toString()));

        var holding = holdingRepository.findByPortfolioIdAndId(portfolioId, holdingId)
                .orElseThrow(() -> new ResourceNotFoundException("Holding", holdingId.toString()));

        holding.setQuantity(req.quantity());
        holding.setAvgCostBasis(req.avgCostBasis());
        holding.setCurrentPrice(req.currentPrice());
        holding.setSector(req.sector());
        holding.setPurchaseDate(req.purchaseDate());

        return toHoldingResponse(holdingRepository.save(holding));
    }

    @Transactional
    public void deleteHolding(UUID portfolioId, UUID userId, UUID holdingId) {
        portfolioRepository.findByIdAndUserIdWithHoldings(portfolioId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", portfolioId.toString()));
        var holding = holdingRepository.findByPortfolioIdAndId(portfolioId, holdingId)
                .orElseThrow(() -> new ResourceNotFoundException("Holding", holdingId.toString()));
        holdingRepository.delete(holding);
    }

    // ── Allocation ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AllocationResponse getAllocation(UUID portfolioId, UUID userId) {
        var portfolio = portfolioRepository.findByIdAndUserIdWithHoldings(portfolioId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", portfolioId.toString()));

        List<Holding> holdings = holdingRepository.findByPortfolioId(portfolioId);
        BigDecimal total = holdings.stream()
                .map(h -> h.getCurrentPrice().multiply(h.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return new AllocationResponse(List.of(), List.of(), BigDecimal.ZERO);
        }

        List<AllocationSlice> bySector = groupAndSlice(holdings, Holding::getSector, total);
        List<AllocationSlice> byAsset  = groupAndSlice(holdings, Holding::getAssetType, total);

        return new AllocationResponse(bySector, byAsset, total);
    }

    private List<AllocationSlice> groupAndSlice(List<Holding> holdings,
            java.util.function.Function<Holding, String> keyFn, BigDecimal total) {
        Map<String, BigDecimal> grouped = new LinkedHashMap<>();
        holdings.forEach(h -> {
            BigDecimal v = h.getCurrentPrice().multiply(h.getQuantity());
            grouped.merge(keyFn.apply(h), v, BigDecimal::add);
        });
        return grouped.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> new AllocationSlice(
                        e.getKey(), e.getValue().setScale(2, RoundingMode.HALF_UP),
                        e.getValue().divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                ))
                .collect(Collectors.toList());
    }

    // ── CSV Export ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public String exportToCsv(UUID portfolioId, UUID userId) {
        var portfolio = portfolioRepository.findByIdAndUserIdWithHoldings(portfolioId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", portfolioId.toString()));

        var sb = new StringBuilder("Symbol,Name,Quantity,Avg Cost,Current Price,Market Value,Unrealized P&L,P&L %,Sector,Asset Type,Purchase Date\n");
        holdingRepository.findByPortfolioId(portfolioId).forEach(h -> {
            BigDecimal mv  = h.getCurrentPrice().multiply(h.getQuantity()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal cost = h.getAvgCostBasis().multiply(h.getQuantity()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal pl  = mv.subtract(cost).setScale(2, RoundingMode.HALF_UP);
            BigDecimal pct = cost.compareTo(BigDecimal.ZERO) != 0
                    ? pl.divide(cost, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            sb.append(String.format("%s,\"%s\",%s,%s,%s,%s,%s,%s%%,%s,%s,%s\n",
                    h.getSymbol(), h.getName(), h.getQuantity(), h.getAvgCostBasis(),
                    h.getCurrentPrice(), mv, pl, pct, h.getSector(), h.getAssetType(), h.getPurchaseDate()));
        });
        return sb.toString();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PortfolioResponse toResponse(Portfolio p) {
        List<Holding> holdings = p.getHoldings();
        BigDecimal totalValue = holdings.stream()
                .map(h -> h.getCurrentPrice().multiply(h.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCost = holdings.stream()
                .map(h -> h.getAvgCostBasis().multiply(h.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pl = totalValue.subtract(totalCost).setScale(2, RoundingMode.HALF_UP);
        BigDecimal plPct = totalCost.compareTo(BigDecimal.ZERO) != 0
                ? pl.divide(totalCost, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new PortfolioResponse(p.getId(), p.getName(), p.getDescription(),
                holdings.stream().map(this::toHoldingResponse).collect(Collectors.toList()),
                totalValue, totalCost, pl, plPct, p.getCreatedAt(), p.getUpdatedAt());
    }

    private HoldingResponse toHoldingResponse(Holding h) {
        BigDecimal mv  = h.getCurrentPrice().multiply(h.getQuantity()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cost = h.getAvgCostBasis().multiply(h.getQuantity()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pl  = mv.subtract(cost).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pct = cost.compareTo(BigDecimal.ZERO) != 0
                ? pl.divide(cost, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new HoldingResponse(h.getId(), h.getSymbol(), h.getName(),
                h.getQuantity(), h.getAvgCostBasis(), h.getCurrentPrice(),
                mv, pl, pct, h.getSector(), h.getAssetType(), h.getPurchaseDate());
    }
}

package com.stockiq.portfolio.controller;

import com.stockiq.portfolio.dto.PortfolioDtos.*;
import com.stockiq.portfolio.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PortfolioController {

    private final PortfolioService portfolioService;

    // ── Portfolios ─────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<PortfolioResponse> create(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreatePortfolioRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(portfolioService.createPortfolio(UUID.fromString(userId), req));
    }

    @GetMapping
    public ResponseEntity<List<PortfolioResponse>> list(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(portfolioService.getUserPortfolios(UUID.fromString(userId)));
    }

    @GetMapping("/{portfolioId}")
    public ResponseEntity<PortfolioResponse> get(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID portfolioId) {
        return ResponseEntity.ok(portfolioService.getPortfolio(portfolioId, UUID.fromString(userId)));
    }

    @PutMapping("/{portfolioId}")
    public ResponseEntity<PortfolioResponse> update(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID portfolioId,
            @Valid @RequestBody UpdatePortfolioRequest req) {
        return ResponseEntity.ok(portfolioService.updatePortfolio(portfolioId, UUID.fromString(userId), req));
    }

    @DeleteMapping("/{portfolioId}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID portfolioId) {
        portfolioService.deletePortfolio(portfolioId, UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    // ── Holdings ──────────────────────────────────────────────────────────────

    @PostMapping("/{portfolioId}/holdings")
    public ResponseEntity<HoldingResponse> addHolding(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID portfolioId,
            @Valid @RequestBody AddHoldingRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(portfolioService.addHolding(portfolioId, UUID.fromString(userId), req));
    }

    @PutMapping("/{portfolioId}/holdings/{holdingId}")
    public ResponseEntity<HoldingResponse> updateHolding(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID portfolioId,
            @PathVariable UUID holdingId,
            @Valid @RequestBody UpdateHoldingRequest req) {
        return ResponseEntity.ok(
                portfolioService.updateHolding(portfolioId, UUID.fromString(userId), holdingId, req));
    }

    @DeleteMapping("/{portfolioId}/holdings/{holdingId}")
    public ResponseEntity<Void> deleteHolding(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID portfolioId,
            @PathVariable UUID holdingId) {
        portfolioService.deleteHolding(portfolioId, UUID.fromString(userId), holdingId);
        return ResponseEntity.noContent().build();
    }

    // ── Allocation & Export ───────────────────────────────────────────────────

    @GetMapping("/{portfolioId}/allocation")
    public ResponseEntity<AllocationResponse> allocation(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID portfolioId) {
        return ResponseEntity.ok(portfolioService.getAllocation(portfolioId, UUID.fromString(userId)));
    }

    @GetMapping("/{portfolioId}/export/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID portfolioId) {
        String csv = portfolioService.exportToCsv(portfolioId, UUID.fromString(userId));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"portfolio-" + portfolioId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }
}

package com.stockiq.portfolio.repository;

import com.stockiq.portfolio.entity.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HoldingRepository extends JpaRepository<Holding, UUID> {

    @Query("SELECT h FROM Holding h WHERE h.portfolio.id = :portfolioId ORDER BY h.symbol")
    List<Holding> findByPortfolioId(UUID portfolioId);

    @Query("SELECT h FROM Holding h WHERE h.portfolio.id = :portfolioId AND h.id = :holdingId")
    Optional<Holding> findByPortfolioIdAndId(UUID portfolioId, UUID holdingId);

    boolean existsByPortfolioIdAndSymbol(UUID portfolioId, String symbol);
}

package com.stockiq.portfolio.repository;

import com.stockiq.portfolio.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {

    List<Portfolio> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT p FROM Portfolio p LEFT JOIN FETCH p.holdings WHERE p.id = :id AND p.userId = :userId")
    Optional<Portfolio> findByIdAndUserIdWithHoldings(@Param("id") UUID id, @Param("userId") UUID userId);

    boolean existsByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);
}

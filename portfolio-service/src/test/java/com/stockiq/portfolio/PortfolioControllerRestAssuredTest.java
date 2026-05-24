package com.stockiq.portfolio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockiq.portfolio.controller.PortfolioController;
import com.stockiq.portfolio.dto.PortfolioDtos.*;
import com.stockiq.portfolio.service.PortfolioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PortfolioController.class)
@DisplayName("PortfolioController — MockMvc Unit Tests")
class PortfolioControllerRestAssuredTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean PortfolioService portfolioService;

    final String userId = UUID.randomUUID().toString();

    PortfolioResponse stubPortfolio() {
        return new PortfolioResponse(
            UUID.randomUUID(), "Test Portfolio", "desc",
            List.of(), BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO,
            Instant.now(), Instant.now()
        );
    }

    HoldingResponse stubHolding() {
        return new HoldingResponse(
            UUID.randomUUID(), "AAPL", "Apple Inc.",
            new BigDecimal("10"), new BigDecimal("150"),
            new BigDecimal("189.84"),
            new BigDecimal("1898.40"), new BigDecimal("398.40"),
            new BigDecimal("26.56"), "Technology", "stock",
            java.time.LocalDate.of(2023,1,15)
        );
    }

    @Test
    @DisplayName("POST /portfolio — creates portfolio, returns 201")
    void createPortfolio_returns201() throws Exception {
        when(portfolioService.createPortfolio(any(), any())).thenReturn(stubPortfolio());

        mockMvc.perform(post("/api/v1/portfolio")
                .header("X-User-Id", userId)
                .header("X-User-Role", "ANALYST")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new CreatePortfolioRequest("Test Portfolio", "desc"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Test Portfolio"));
    }

    @Test
    @DisplayName("GET /portfolio — returns list")
    void getPortfolios_returnsList() throws Exception {
        when(portfolioService.getUserPortfolios(any())).thenReturn(List.of(stubPortfolio()));

        mockMvc.perform(get("/api/v1/portfolio")
                .header("X-User-Id", userId)
                .header("X-User-Role", "ANALYST"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("POST /portfolio/{id}/holdings — adds holding, returns 201")
    void addHolding_returns201() throws Exception {
        UUID portfolioId = UUID.randomUUID();
        when(portfolioService.addHolding(any(), any(), any())).thenReturn(stubHolding());

        mockMvc.perform(post("/api/v1/portfolio/" + portfolioId + "/holdings")
                .header("X-User-Id", userId)
                .header("X-User-Role", "ANALYST")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddHoldingRequest(
                        "AAPL", "Apple Inc.", new BigDecimal("10"),
                        new BigDecimal("150"), new BigDecimal("189.84"),
                        "Technology", "stock", java.time.LocalDate.of(2023,1,15)))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.symbol").value("AAPL"))
            .andExpect(jsonPath("$.unrealizedPL").value(398.40));
    }

    @Test
    @DisplayName("GET /portfolio/{id}/allocation — returns allocation")
    void getAllocation_returns200() throws Exception {
        UUID portfolioId = UUID.randomUUID();
        when(portfolioService.getAllocation(any(), any())).thenReturn(
                new AllocationResponse(List.of(), List.of(), BigDecimal.ZERO));

        mockMvc.perform(get("/api/v1/portfolio/" + portfolioId + "/allocation")
                .header("X-User-Id", userId)
                .header("X-User-Role", "ANALYST"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /portfolio/{id} — returns 204")
    void deletePortfolio_returns204() throws Exception {
        UUID portfolioId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/portfolio/" + portfolioId)
                .header("X-User-Id", userId)
                .header("X-User-Role", "ANALYST"))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /portfolio — 400 for blank name")
    void createPortfolio_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/portfolio")
                .header("X-User-Id", userId)
                .header("X-User-Role", "ANALYST")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new CreatePortfolioRequest("", null))))
            .andExpect(status().isBadRequest());
    }
}

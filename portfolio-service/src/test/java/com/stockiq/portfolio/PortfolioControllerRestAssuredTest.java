package com.stockiq.portfolio;

import com.stockiq.portfolio.dto.PortfolioDtos.*;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PortfolioControllerRestAssuredTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("stockiq_test")
            .withUsername("stockiq")
            .withPassword("stockiq_test");

    @LocalServerPort
    int port;

    static String createdPortfolioId;
    static String createdHoldingId;
    final String userId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1/portfolio";
    }

    @Test @Order(1)
    @DisplayName("POST /portfolio — creates portfolio and returns 201")
    void createPortfolio_returns201() {
        createdPortfolioId = given()
                .contentType(ContentType.JSON)
                .header("X-User-Id", userId)
                .header("X-User-Role", "ANALYST")
                .body(new CreatePortfolioRequest("Test Portfolio", "Integration test portfolio"))
                .when().post()
                .then()
                .statusCode(201)
                .body("name", equalTo("Test Portfolio"))
                .body("id", notNullValue())
                .body("totalValue", equalTo(0))
                .extract().path("id");
    }

    @Test @Order(2)
    @DisplayName("GET /portfolio — returns user portfolios list")
    void getPortfolios_returnsList() {
        given()
                .header("X-User-Id", userId)
                .header("X-User-Role", "ANALYST")
                .when().get()
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test @Order(3)
    @DisplayName("POST /portfolio/{id}/holdings — adds holding returns 201")
    void addHolding_returns201() {
        var req = new AddHoldingRequest("AAPL", "Apple Inc.", new BigDecimal("10"),
                new BigDecimal("150.00"), new BigDecimal("189.84"),
                "Technology", "stock", LocalDate.of(2023, 1, 15));

        createdHoldingId = given()
                .contentType(ContentType.JSON)
                .header("X-User-Id", userId)
                .header("X-User-Role", "ANALYST")
                .body(req)
                .when().post("/" + createdPortfolioId + "/holdings")
                .then()
                .statusCode(201)
                .body("symbol", equalTo("AAPL"))
                .body("marketValue", equalTo(1898.40f))
                .body("unrealizedPL", greaterThan(0f))
                .extract().path("id");
    }

    @Test @Order(4)
    @DisplayName("GET /portfolio/{id} — returns portfolio with P&L calculated")
    void getPortfolio_withHoldings_returnsPL() {
        given()
                .header("X-User-Id", userId)
                .header("X-User-Role", "ANALYST")
                .when().get("/" + createdPortfolioId)
                .then()
                .statusCode(200)
                .body("holdings.size()", equalTo(1))
                .body("totalValue", equalTo(1898.40f))
                .body("unrealizedPL", notNullValue());
    }

    @Test @Order(5)
    @DisplayName("GET /portfolio/{id}/allocation — returns sector allocation")
    void getAllocation_returnsSectors() {
        given()
                .header("X-User-Id", userId)
                .header("X-User-Role", "ANALYST")
                .when().get("/" + createdPortfolioId + "/allocation")
                .then()
                .statusCode(200)
                .body("bySector.size()", equalTo(1))
                .body("bySector[0].label", equalTo("Technology"))
                .body("bySector[0].percent", equalTo(100.00f));
    }

    @Test @Order(6)
    @DisplayName("GET /portfolio/{id}/export/csv — returns CSV file")
    void exportCsv_returnsCsvContent() {
        given()
                .header("X-User-Id", userId)
                .header("X-User-Role", "ANALYST")
                .when().get("/" + createdPortfolioId + "/export/csv")
                .then()
                .statusCode(200)
                .contentType("text/csv")
                .body(containsString("AAPL"))
                .body(containsString("Apple Inc."));
    }

    @Test @Order(7)
    @DisplayName("DELETE /portfolio/{id}/holdings/{hId} — removes holding")
    void deleteHolding_returns204() {
        given()
                .header("X-User-Id", userId)
                .header("X-User-Role", "ANALYST")
                .when().delete("/" + createdPortfolioId + "/holdings/" + createdHoldingId)
                .then()
                .statusCode(204);
    }

    @Test @Order(8)
    @DisplayName("GET /portfolio/{id} — 404 for wrong user")
    void getPortfolio_wrongUser_returns404() {
        given()
                .header("X-User-Id", UUID.randomUUID().toString())
                .header("X-User-Role", "ANALYST")
                .when().get("/" + createdPortfolioId)
                .then()
                .statusCode(404);
    }

    @Test @Order(9)
    @DisplayName("POST /portfolio — 400 for blank name")
    void createPortfolio_blankName_returns400() {
        given()
                .contentType(ContentType.JSON)
                .header("X-User-Id", userId)
                .header("X-User-Role", "ANALYST")
                .body(new CreatePortfolioRequest("", null))
                .when().post()
                .then()
                .statusCode(400)
                .body("error", equalTo("Validation failed"));
    }
}

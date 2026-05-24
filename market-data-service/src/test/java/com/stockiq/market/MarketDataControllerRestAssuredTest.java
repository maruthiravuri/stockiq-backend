package com.stockiq.market;

import io.restassured.RestAssured;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Market Data API — RestAssured Integration Tests")
class MarketDataControllerRestAssuredTest {

    @LocalServerPort int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1/market";
    }

    @Test
    @DisplayName("GET /quote/{symbol} — returns quote with price > 0")
    void getQuote_validSymbol_returnsQuote() {
        given()
                .header("X-User-Id", "test-user")
                .header("X-User-Role", "ANALYST")
                .when().get("/quote/AAPL")
                .then()
                .statusCode(200)
                .body("symbol", equalTo("AAPL"))
                .body("price", greaterThan(0f))
                .body("timestamp", notNullValue());
    }

    @Test
    @DisplayName("GET /mag7 — returns 7 quotes")
    void getMag7_returns7Quotes() {
        given()
                .header("X-User-Id", "test-user")
                .header("X-User-Role", "ANALYST")
                .when().get("/mag7")
                .then()
                .statusCode(200)
                .body("size()", equalTo(7))
                .body("symbol", hasItems("AAPL", "MSFT", "NVDA", "GOOGL", "AMZN", "META", "TSLA"));
    }

    @Test
    @DisplayName("GET /quotes?symbols= — returns batch quotes")
    void getBatchQuotes_returnsAll() {
        given()
                .header("X-User-Id", "test-user")
                .header("X-User-Role", "ANALYST")
                .queryParam("symbols", "VOO,GLD,SCHD")
                .when().get("/quotes")
                .then()
                .statusCode(200)
                .body("size()", equalTo(3));
    }

    @Test
    @DisplayName("GET /crypto — returns 5 crypto assets")
    void getCrypto_returns5() {
        given()
                .header("X-User-Id", "test-user")
                .header("X-User-Role", "ANALYST")
                .when().get("/crypto")
                .then()
                .statusCode(200)
                .body("size()", equalTo(5));
    }
}

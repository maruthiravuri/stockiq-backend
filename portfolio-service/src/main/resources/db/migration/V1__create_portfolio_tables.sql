CREATE TABLE portfolios (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    user_id     UUID         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE holdings (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    portfolio_id   UUID         NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    symbol         VARCHAR(20)  NOT NULL,
    name           VARCHAR(200) NOT NULL,
    quantity       NUMERIC(15,4) NOT NULL CHECK (quantity > 0),
    avg_cost_basis NUMERIC(15,4) NOT NULL CHECK (avg_cost_basis > 0),
    sector         VARCHAR(50)  NOT NULL,
    asset_type     VARCHAR(20)  NOT NULL,
    purchase_date  DATE         NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_portfolios_user_id  ON portfolios(user_id);
CREATE INDEX idx_holdings_portfolio  ON holdings(portfolio_id);
CREATE INDEX idx_holdings_symbol     ON holdings(symbol);

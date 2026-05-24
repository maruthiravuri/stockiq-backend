ALTER TABLE holdings
    ADD COLUMN IF NOT EXISTS current_price NUMERIC(15,4) NOT NULL DEFAULT 0
        CHECK (current_price >= 0);

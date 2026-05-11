-- Add real-world processing timestamp to colonies.
-- Used by the catch-up system to calculate missed ticks.
ALTER TABLE colonies
    ADD COLUMN last_processed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    AFTER last_tick_at;

-- Sync existing rows with their last_tick_at value
UPDATE colonies SET last_processed_at = last_tick_at;
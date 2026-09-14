ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS auction_round INTEGER DEFAULT 1;

ALTER TABLE IF EXISTS players
    ADD COLUMN IF NOT EXISTS re_auctioned BOOLEAN DEFAULT FALSE;

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS rtm_lockdown_player VARCHAR(255);

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS market_adjustment_player VARCHAR(255);

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS market_adjustment INTEGER DEFAULT 0;

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS protection_bonus INTEGER DEFAULT 300;

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS protection_penalty INTEGER DEFAULT 200;

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS protection_selection_enabled BOOLEAN DEFAULT FALSE;

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS value_bet_player VARCHAR(255);

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS value_bet_events_used INTEGER DEFAULT 0;

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS pending_silent_auction_player VARCHAR(255);

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS pending_random_event_type VARCHAR(255);

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS pending_random_event_player VARCHAR(255);

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS pending_random_event_title VARCHAR(255);

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS pending_random_event_description VARCHAR(255);

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS pending_random_event_amount INTEGER;

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS market_crash_applied BOOLEAN DEFAULT FALSE;

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS wheel_spin_started_at BIGINT;

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS wheel_spin_ends_at BIGINT;

ALTER TABLE IF EXISTS auction_config
    ALTER COLUMN auction_phase VARCHAR(255);

CREATE TABLE IF NOT EXISTS current_auction (
    id BIGINT PRIMARY KEY,
    current_player VARCHAR(255),
    player_seed VARCHAR(255),
    current_bid INTEGER NOT NULL DEFAULT 0,
    leader VARCHAR(255),
    base_price INTEGER NOT NULL DEFAULT 0
);

ALTER TABLE IF EXISTS random_events
    ALTER COLUMN type VARCHAR(255);
ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS auction_round INTEGER DEFAULT 1;

ALTER TABLE IF EXISTS players
    ADD COLUMN IF NOT EXISTS re_auctioned BOOLEAN DEFAULT FALSE;

ALTER TABLE IF EXISTS players
    ADD COLUMN IF NOT EXISTS deferred_to_re_auction BOOLEAN DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS re_auction_snapshots (
    id BIGINT PRIMARY KEY,
    state_json CLOB NOT NULL
);

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS rtm_lockdown_player VARCHAR(255);

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS market_adjustment_player VARCHAR(255);

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS market_adjustment INTEGER DEFAULT 0;

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS curse_selection_enabled BOOLEAN DEFAULT FALSE;

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS blind_bonanza_awards_used INTEGER DEFAULT 0;

ALTER TABLE IF EXISTS teams
    ADD COLUMN IF NOT EXISTS tournament_points INTEGER DEFAULT 0;

ALTER TABLE IF EXISTS blind_opening_bids
    ADD COLUMN IF NOT EXISTS bonus_eligible BOOLEAN DEFAULT FALSE;

ALTER TABLE IF EXISTS blind_opening_bids
    ADD COLUMN IF NOT EXISTS bonus_awarded BOOLEAN DEFAULT FALSE;

ALTER TABLE IF EXISTS blind_opening_bids
    ADD COLUMN IF NOT EXISTS bonus_awarded_amount INTEGER DEFAULT 0;

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
    ADD COLUMN IF NOT EXISTS next_nomination_captain VARCHAR(255);

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS bid_deadline_at BIGINT;

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS auction_timeout_ends_at BIGINT;

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS paused_bid_time_remaining_millis BIGINT;

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS timeout_captain_name VARCHAR(255);

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS auction_break_ends_at BIGINT;

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS auctions_since_last_break INTEGER DEFAULT 0 NOT NULL;

ALTER TABLE IF EXISTS auction_config
    ADD COLUMN IF NOT EXISTS bid_timer_seconds INTEGER DEFAULT 10 NOT NULL;

CREATE TABLE IF NOT EXISTS auction_timeout_usage (
    auction_config_id BIGINT NOT NULL,
    captain_name VARCHAR(255) NOT NULL,
    timeouts_used INTEGER
);

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
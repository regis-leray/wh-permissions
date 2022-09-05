CREATE TABLE IF NOT EXISTS permissions(
 id                UUID PRIMARY KEY,
 player_id         VARCHAR(255) NOT NULL,
 current_state     TEXT NOT NULL,
 old_state         TEXT NOT NULL,
 created_at        TIMESTAMPTZ NOT NULL,
 updated_at        TIMESTAMPTZ NOT NULL,

 UNIQUE (player_id)
);
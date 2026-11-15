CREATE TABLE social_write_quotas (
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action varchar(32) NOT NULL,
    started_at timestamptz NOT NULL,
    used integer NOT NULL CHECK (used > 0),
    PRIMARY KEY (user_id, action)
);

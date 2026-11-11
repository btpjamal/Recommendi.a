CREATE TABLE social_follows (
    follower_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    followed_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follower_id, followed_id),
    CONSTRAINT ck_social_follow_other CHECK (follower_id <> followed_id)
);

CREATE INDEX idx_social_follows_followed ON social_follows (followed_id, follower_id);

-- Blocking ends both follow relationships. Unblocking requires a new explicit follow.
CREATE FUNCTION remove_blocked_follows() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    DELETE FROM social_follows
    WHERE (follower_id = NEW.blocker_id AND followed_id = NEW.blocked_id)
       OR (follower_id = NEW.blocked_id AND followed_id = NEW.blocker_id);
    RETURN NEW;
END;
$$;

CREATE TRIGGER social_block_removes_follows
AFTER INSERT ON social_user_blocks
FOR EACH ROW EXECUTE FUNCTION remove_blocked_follows();

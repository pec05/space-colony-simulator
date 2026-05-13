-- ============================================================
--  Link colonies to real users via FK
-- ============================================================
ALTER TABLE colonies
    ADD COLUMN user_id BIGINT NULL AFTER owner_id,
    ADD CONSTRAINT fk_colonies_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL;

CREATE INDEX idx_colonies_user_id ON colonies(user_id);
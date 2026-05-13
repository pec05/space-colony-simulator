-- ============================================================
--  Users table — identity without authentication
-- ============================================================
CREATE TABLE users (
                       id          BIGINT          NOT NULL AUTO_INCREMENT,
                       username    VARCHAR(50)     NOT NULL,
                       email       VARCHAR(150)    NOT NULL,
                       created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP,

                       CONSTRAINT pk_users             PRIMARY KEY (id),
                       CONSTRAINT uq_users_username    UNIQUE (username),
                       CONSTRAINT uq_users_email       UNIQUE (email)
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email    ON users(email);
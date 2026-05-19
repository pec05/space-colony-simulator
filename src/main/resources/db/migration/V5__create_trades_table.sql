-- ============================================================
--  Trades table — in-transit resource transfers between colonies
--  Resources leave the sender IMMEDIATELY on trade creation.
--  They arrive at the receiver after etaSimTime is reached.
--  If receiver is destroyed before arrival → status = FAILED.
-- ============================================================
CREATE TABLE trades (
                        id                  BIGINT          NOT NULL AUTO_INCREMENT,
                        sender_colony_id    BIGINT          NOT NULL,
                        receiver_colony_id  BIGINT          NOT NULL,
                        resource_type       VARCHAR(20)     NOT NULL,
                        amount              DECIMAL(12, 2)  NOT NULL,
                        status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
                        initiated_sim_time  DATETIME        NOT NULL,
                        eta_sim_time        DATETIME        NOT NULL,
                        arrived_at          DATETIME,
                        created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        CONSTRAINT pk_trades                PRIMARY KEY (id),
                        CONSTRAINT fk_trade_sender          FOREIGN KEY (sender_colony_id)
                            REFERENCES colonies(id),
                        CONSTRAINT fk_trade_receiver        FOREIGN KEY (receiver_colony_id)
                            REFERENCES colonies(id),
                        CONSTRAINT chk_trade_status         CHECK (status IN
                                                                   ('PENDING', 'ARRIVED', 'CANCELLED', 'FAILED')),
                        CONSTRAINT chk_trade_not_self       CHECK (sender_colony_id != receiver_colony_id),
    CONSTRAINT chk_trade_amount         CHECK (amount > 0)
);

CREATE INDEX idx_trades_sender    ON trades(sender_colony_id);
CREATE INDEX idx_trades_receiver  ON trades(receiver_colony_id);
CREATE INDEX idx_trades_pending   ON trades(receiver_colony_id, status, eta_sim_time);
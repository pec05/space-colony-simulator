-- ============================================================
--  Space Colony Simulator — Initial Schema
--  V1: colonies, colony_resources, colony_events
-- ============================================================

-- ------------------------------------------------------------
--  COLONIES
--  Core entity. last_tick_at is the heartbeat of the simulation:
--  it tells us when this colony was last processed so we can
--  calculate exactly how many ticks to catch up on reconnection.
-- ------------------------------------------------------------
CREATE TABLE colonies (
                          id              BIGINT          NOT NULL AUTO_INCREMENT,
                          name            VARCHAR(100)    NOT NULL,
                          owner_id        VARCHAR(100)    NOT NULL,       -- FK to users table (Phase 3)
                          population      INT             NOT NULL DEFAULT 10,
                          status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
                          founded_at      DATETIME        NOT NULL,
                          last_tick_at    DATETIME        NOT NULL,       -- critical for offline catch-up
                          created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                              ON UPDATE CURRENT_TIMESTAMP,

                          CONSTRAINT pk_colonies          PRIMARY KEY (id),
                          CONSTRAINT chk_colony_status    CHECK (status IN ('ACTIVE', 'ABANDONED', 'DESTROYED')),
                          CONSTRAINT chk_population       CHECK (population >= 0)
);

-- ------------------------------------------------------------
--  COLONY_RESOURCES
--  One row per resource type per colony.
--  production_rate and consumption_rate are per-tick values.
--  The simulation engine applies: current = current + production - consumption
--  clamped to [0, storage_capacity].
-- ------------------------------------------------------------
CREATE TABLE colony_resources (
                                  id                  BIGINT          NOT NULL AUTO_INCREMENT,
                                  colony_id           BIGINT          NOT NULL,
                                  resource_type       VARCHAR(20)     NOT NULL,
                                  current_amount      DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
                                  production_rate     DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
                                  consumption_rate    DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
                                  storage_capacity    DECIMAL(12, 2)  NOT NULL DEFAULT 1000.00,
                                  updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                      ON UPDATE CURRENT_TIMESTAMP,

                                  CONSTRAINT pk_colony_resources      PRIMARY KEY (id),
                                  CONSTRAINT fk_resource_colony       FOREIGN KEY (colony_id)
                                      REFERENCES colonies(id)
                                      ON DELETE CASCADE,
                                  CONSTRAINT uq_colony_resource_type  UNIQUE (colony_id, resource_type),
                                  CONSTRAINT chk_resource_type        CHECK (resource_type IN
                                                                             ('OXYGEN', 'FOOD', 'ENERGY', 'MATERIALS')),
                                  CONSTRAINT chk_current_amount       CHECK (current_amount >= 0),
                                  CONSTRAINT chk_production_rate      CHECK (production_rate >= 0),
                                  CONSTRAINT chk_consumption_rate     CHECK (consumption_rate >= 0),
                                  CONSTRAINT chk_storage_capacity     CHECK (storage_capacity > 0)
);

-- ------------------------------------------------------------
--  COLONY_EVENTS
--  Records everything that happens to a colony during simulation.
--  sim_occurred_at is SIMULATION time, not real time.
--  This is what enables the replay feature later.
-- ------------------------------------------------------------
CREATE TABLE colony_events (
                               id                  BIGINT          NOT NULL AUTO_INCREMENT,
                               colony_id           BIGINT          NOT NULL,
                               event_type          VARCHAR(30)     NOT NULL,
                               severity            VARCHAR(10)     NOT NULL DEFAULT 'LOW',
                               description         TEXT,
                               sim_occurred_at     DATETIME        NOT NULL,   -- when it happened in sim time
                               sim_resolved_at     DATETIME,                   -- null = still active
                               is_resolved         BOOLEAN         NOT NULL DEFAULT FALSE,
                               created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT pk_colony_events     PRIMARY KEY (id),
                               CONSTRAINT fk_event_colony      FOREIGN KEY (colony_id)
                                   REFERENCES colonies(id)
                                   ON DELETE CASCADE,
                               CONSTRAINT chk_event_type       CHECK (event_type IN (
                                                                                     'RESOURCE_SHORTAGE',
                                                                                     'OXYGEN_SHORTAGE',
                                                                                     'FOOD_SHORTAGE',
                                                                                     'ENERGY_FAILURE',
                                                                                     'MATERIAL_SHORTAGE',
                                                                                     'POPULATION_DECLINE',
                                                                                     'CATASTROPHE'
                                   )),
                               CONSTRAINT chk_severity         CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

-- ------------------------------------------------------------
--  INDEXES
--  Added for the queries the simulation engine will run most:
--  - finding all active colonies to tick
--  - fetching resources for a colony
--  - fetching unresolved events per colony
-- ------------------------------------------------------------
CREATE INDEX idx_colonies_status        ON colonies(status);
CREATE INDEX idx_colonies_owner         ON colonies(owner_id);
CREATE INDEX idx_resources_colony       ON colony_resources(colony_id);
CREATE INDEX idx_events_colony          ON colony_events(colony_id);
CREATE INDEX idx_events_unresolved      ON colony_events(colony_id, is_resolved);
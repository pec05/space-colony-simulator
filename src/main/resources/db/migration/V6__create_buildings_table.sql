-- ============================================================
--  Buildings — structures that boost resource production rates.
--  When a building is constructed, production_rate in
--  colony_resources is updated immediately
--  Each row represents one physical building in a colony.
-- ============================================================
CREATE TABLE buildings (
                           id          BIGINT      NOT NULL AUTO_INCREMENT,
                           colony_id   BIGINT      NOT NULL,
                           type        VARCHAR(30) NOT NULL,
                           built_at    DATETIME    NOT NULL,
                           created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT pk_buildings         PRIMARY KEY (id),
                           CONSTRAINT fk_buildings_colony  FOREIGN KEY (colony_id)
                               REFERENCES colonies(id)
                               ON DELETE CASCADE,
                           CONSTRAINT chk_building_type    CHECK (type IN
                                                                  ('LIFE_SUPPORT', 'FARM', 'REACTOR', 'MINE'))
);

CREATE INDEX idx_buildings_colony ON buildings(colony_id);
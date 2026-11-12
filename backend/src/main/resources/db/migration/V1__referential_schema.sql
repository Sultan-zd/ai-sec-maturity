-- =====================================================================
-- V1 — Schéma du référentiel AIMA
-- Un référentiel est versionné et immuable : une évaluation référence
-- toujours la version exacte utilisée, ce qui garantit la comparabilité.
-- =====================================================================

CREATE TABLE ref_referential (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code                 VARCHAR(50)   NOT NULL,
    version              VARCHAR(20)   NOT NULL,
    name                 VARCHAR(255)  NOT NULL,
    description          VARCHAR(1000),
    default_target_level INTEGER       NOT NULL CHECK (default_target_level BETWEEN 1 AND 5),
    active               BOOLEAN       NOT NULL DEFAULT FALSE,
    imported_at          TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_referential_code_version UNIQUE (code, version)
);

-- Un seul référentiel actif à la fois
CREATE UNIQUE INDEX uk_referential_single_active ON ref_referential (active) WHERE active;

CREATE TABLE ref_source (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    referential_id  BIGINT       NOT NULL REFERENCES ref_referential (id) ON DELETE CASCADE,
    code            VARCHAR(50)  NOT NULL,
    name            VARCHAR(255) NOT NULL,
    CONSTRAINT uk_source UNIQUE (referential_id, code)
);

CREATE TABLE ref_maturity_level (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    referential_id  BIGINT        NOT NULL REFERENCES ref_referential (id) ON DELETE CASCADE,
    level           INTEGER       NOT NULL CHECK (level BETWEEN 1 AND 5),
    name            VARCHAR(100)  NOT NULL,
    min_score       NUMERIC(3, 2) NOT NULL,
    description     VARCHAR(1000) NOT NULL,
    CONSTRAINT uk_maturity_level UNIQUE (referential_id, level)
);

CREATE TABLE ref_scoring_cap (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    referential_id  BIGINT        NOT NULL REFERENCES ref_referential (id) ON DELETE CASCADE,
    criticality     INTEGER       NOT NULL CHECK (criticality BETWEEN 1 AND 3),
    answer_level    INTEGER       NOT NULL CHECK (answer_level BETWEEN 1 AND 5),
    domain_cap      NUMERIC(3, 2) NOT NULL,
    CONSTRAINT uk_scoring_cap UNIQUE (referential_id, criticality, answer_level)
);

CREATE TABLE ref_domain (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    referential_id  BIGINT        NOT NULL REFERENCES ref_referential (id) ON DELETE CASCADE,
    code            VARCHAR(20)   NOT NULL,
    name            VARCHAR(255)  NOT NULL,
    description     VARCHAR(1000),
    weight          NUMERIC(4, 3) NOT NULL CHECK (weight > 0 AND weight <= 1),
    display_order   INTEGER       NOT NULL,
    CONSTRAINT uk_domain UNIQUE (referential_id, code)
);

CREATE TABLE ref_subdomain (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    domain_id       BIGINT        NOT NULL REFERENCES ref_domain (id) ON DELETE CASCADE,
    code            VARCHAR(20)   NOT NULL,
    name            VARCHAR(255)  NOT NULL,
    description     VARCHAR(1000),
    weight          NUMERIC(4, 3) NOT NULL DEFAULT 1,
    display_order   INTEGER       NOT NULL,
    CONSTRAINT uk_subdomain UNIQUE (domain_id, code)
);

-- Correspondance avec les normes sources (ex. ISO42001 → A.2.2)
CREATE TABLE ref_subdomain_reference (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subdomain_id    BIGINT       NOT NULL REFERENCES ref_subdomain (id) ON DELETE CASCADE,
    source_code     VARCHAR(50)  NOT NULL,
    clause          VARCHAR(100) NOT NULL,
    display_order   INTEGER      NOT NULL
);

CREATE TABLE ref_question (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subdomain_id    BIGINT        NOT NULL REFERENCES ref_subdomain (id) ON DELETE CASCADE,
    code            VARCHAR(20)   NOT NULL,
    text            VARCHAR(1000) NOT NULL,
    evidence        VARCHAR(1000) NOT NULL,
    criticality     INTEGER       NOT NULL CHECK (criticality BETWEEN 1 AND 3),
    weight          INTEGER       NOT NULL CHECK (weight > 0),
    effort          VARCHAR(10)   NOT NULL CHECK (effort IN ('LOW', 'MEDIUM', 'HIGH')),
    display_order   INTEGER       NOT NULL,
    CONSTRAINT uk_question UNIQUE (subdomain_id, code)
);

-- Description de chaque niveau (1..5) et recommandation pour passer au niveau suivant (NULL pour le niveau 5)
CREATE TABLE ref_question_level (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    question_id     BIGINT        NOT NULL REFERENCES ref_question (id) ON DELETE CASCADE,
    level           INTEGER       NOT NULL CHECK (level BETWEEN 1 AND 5),
    description     VARCHAR(1000) NOT NULL,
    recommendation  VARCHAR(1000),
    CONSTRAINT uk_question_level UNIQUE (question_id, level)
);

CREATE INDEX idx_source_referential   ON ref_source (referential_id);
CREATE INDEX idx_level_referential    ON ref_maturity_level (referential_id);
CREATE INDEX idx_cap_referential      ON ref_scoring_cap (referential_id);
CREATE INDEX idx_domain_referential   ON ref_domain (referential_id);
CREATE INDEX idx_subdomain_domain     ON ref_subdomain (domain_id);
CREATE INDEX idx_reference_subdomain  ON ref_subdomain_reference (subdomain_id);
CREATE INDEX idx_question_subdomain   ON ref_question (subdomain_id);
CREATE INDEX idx_qlevel_question      ON ref_question_level (question_id);

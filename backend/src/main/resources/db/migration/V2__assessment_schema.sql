-- =====================================================================
-- V2 — Organisations, évaluations et réponses
-- =====================================================================

CREATE TABLE organization (
    id            UUID PRIMARY KEY,
    name          VARCHAR(255)  NOT NULL,
    sector        VARCHAR(30)   NOT NULL,
    company_size  VARCHAR(10)   NOT NULL CHECK (company_size IN ('TPE', 'PME', 'ETI', 'GE')),
    country       VARCHAR(100),
    description   VARCHAR(2000),
    created_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE TABLE assessment (
    id               UUID PRIMARY KEY,
    organization_id  UUID          NOT NULL REFERENCES organization (id),
    referential_id   BIGINT        NOT NULL REFERENCES ref_referential (id),
    title            VARCHAR(255)  NOT NULL,
    scope            VARCHAR(2000),
    status           VARCHAR(20)   NOT NULL CHECK (status IN ('DRAFT', 'SUBMITTED')),
    -- Score global figé à la soumission (pour l'historique et les listes)
    global_score     NUMERIC(3, 2),
    global_level     INTEGER CHECK (global_level BETWEEN 1 AND 5),
    created_at       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    submitted_at     TIMESTAMP(6) WITH TIME ZONE,
    version          BIGINT        NOT NULL DEFAULT 0     -- verrouillage optimiste
);

-- Niveau cible par domaine (ex. GOV → 3, LLM → 4)
CREATE TABLE assessment_target (
    assessment_id  UUID        NOT NULL REFERENCES assessment (id) ON DELETE CASCADE,
    domain_code    VARCHAR(20) NOT NULL,
    target_level   INTEGER     NOT NULL CHECK (target_level BETWEEN 1 AND 5),
    PRIMARY KEY (assessment_id, domain_code)
);

CREATE TABLE answer (
    id              UUID PRIMARY KEY,
    assessment_id   UUID          NOT NULL REFERENCES assessment (id) ON DELETE CASCADE,
    question_id     BIGINT        NOT NULL REFERENCES ref_question (id),
    level           INTEGER CHECK (level BETWEEN 1 AND 5),
    not_applicable  BOOLEAN       NOT NULL DEFAULT FALSE,
    justification   VARCHAR(1000),
    comment         VARCHAR(2000),
    evidence        VARCHAR(1000),          -- référence de la preuve : lien, nom de document…
    created_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    version         BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uk_answer UNIQUE (assessment_id, question_id),
    -- Soit « Non applicable » justifié et sans niveau, soit un niveau renseigné
    CONSTRAINT ck_answer_consistency CHECK (
        (not_applicable AND level IS NULL AND justification IS NOT NULL)
        OR (NOT not_applicable AND level IS NOT NULL)
    )
);

CREATE INDEX idx_assessment_organization ON assessment (organization_id);
CREATE INDEX idx_assessment_referential  ON assessment (referential_id);
CREATE INDEX idx_answer_question         ON answer (question_id);

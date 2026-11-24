-- =====================================================================
-- V4 — Mesures techniques (labo red-team)
-- Le labo d'attaque mesure le niveau réel d'une question (ex. injection de
-- prompt réussie → LLM.1.1 mesuré au niveau 1). Une mesure appartient à UNE
-- évaluation : elle n'est jamais écrite dans le référentiel.
-- Une seule mesure par question et par évaluation : un nouveau passage
-- (ex. après correction de la faille) remplace la mesure précédente.
-- =====================================================================

CREATE TABLE measurement (
    id              UUID PRIMARY KEY,
    assessment_id   UUID          NOT NULL REFERENCES assessment (id) ON DELETE CASCADE,
    question_id     BIGINT        NOT NULL REFERENCES ref_question (id),
    measured_level  INTEGER       NOT NULL CHECK (measured_level BETWEEN 1 AND 5),
    attack_family   VARCHAR(40)   NOT NULL,
    tool            VARCHAR(100)  NOT NULL,          -- ex. « garak 0.12 », « promptfoo »
    run_id          VARCHAR(100)  NOT NULL,          -- identifiant du passage de tests côté labo
    evidence        VARCHAR(2000) NOT NULL,          -- résumé du résultat : « 14/20 charges réussies »…
    evidence_url    VARCHAR(1000),                   -- lien vers le rapport complet du labo
    measured_at     TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    recorded_by     UUID          REFERENCES app_user (id),
    created_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    version         BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uk_measurement UNIQUE (assessment_id, question_id)
);

CREATE INDEX idx_measurement_assessment ON measurement (assessment_id);

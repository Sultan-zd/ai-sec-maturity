-- =====================================================================
-- V3 — Utilisateurs, rôles et mode audit
-- =====================================================================

CREATE TABLE app_user (
    id               UUID PRIMARY KEY,
    email            VARCHAR(255) NOT NULL,
    full_name        VARCHAR(255) NOT NULL,
    password_hash    VARCHAR(255) NOT NULL,                 -- BCrypt, jamais le mot de passe en clair
    role             VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'AUDITOR', 'ORG_USER')),
    organization_id  UUID REFERENCES organization (id),
    enabled          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    last_login_at    TIMESTAMP(6) WITH TIME ZONE,
    -- Un utilisateur « organisation » est forcément rattaché à une organisation
    CONSTRAINT ck_user_org CHECK (role <> 'ORG_USER' OR organization_id IS NOT NULL)
);

CREATE UNIQUE INDEX uk_app_user_email ON app_user (LOWER(email));

-- Nouveau statut VALIDATED : l'auditeur a revu toutes les réponses
ALTER TABLE assessment DROP CONSTRAINT assessment_status_check;
ALTER TABLE assessment ADD CONSTRAINT assessment_status_check
    CHECK (status IN ('DRAFT', 'SUBMITTED', 'VALIDATED'));

ALTER TABLE assessment
    ADD COLUMN created_by    UUID REFERENCES app_user (id),
    ADD COLUMN validated_by  UUID REFERENCES app_user (id),
    ADD COLUMN validated_at  TIMESTAMP(6) WITH TIME ZONE;

-- Revue de chaque réponse par l'auditeur
ALTER TABLE answer
    ADD COLUMN review_status    VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (review_status IN ('PENDING', 'CONFIRMED', 'ADJUSTED')),
    ADD COLUMN audited_level    INTEGER CHECK (audited_level BETWEEN 1 AND 5),
    ADD COLUMN auditor_comment  VARCHAR(2000),
    ADD COLUMN reviewed_by      UUID REFERENCES app_user (id),
    ADD COLUMN reviewed_at      TIMESTAMP(6) WITH TIME ZONE,
    -- Une réponse corrigée porte obligatoirement le niveau retenu par l'auditeur
    ADD CONSTRAINT ck_answer_review CHECK (review_status <> 'ADJUSTED' OR audited_level IS NOT NULL);

CREATE INDEX idx_app_user_organization ON app_user (organization_id);

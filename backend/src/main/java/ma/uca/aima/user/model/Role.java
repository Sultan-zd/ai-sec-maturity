package ma.uca.aima.user.model;

/** Rôles de la plateforme. */
public enum Role {
    /** Administre la plateforme : utilisateurs, organisations, toutes les évaluations. */
    ADMIN,
    /** Auditeur : consulte les évaluations soumises, valide ou corrige les réponses. */
    AUDITOR,
    /** Membre d'une organisation : réalise l'auto-évaluation de SA propre organisation. */
    ORG_USER
}

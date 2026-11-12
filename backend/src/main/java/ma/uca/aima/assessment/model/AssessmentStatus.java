package ma.uca.aima.assessment.model;

/** Cycle de vie d'une évaluation : DRAFT → SUBMITTED → VALIDATED. */
public enum AssessmentStatus {
    /** En cours de saisie : les réponses et les cibles sont modifiables. */
    DRAFT,
    /** Soumise : réponses verrouillées, en attente de revue par un auditeur. */
    SUBMITTED,
    /** Validée par un auditeur : état final, le score « audité » est figé. */
    VALIDATED
}

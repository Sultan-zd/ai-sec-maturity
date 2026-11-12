package ma.uca.aima.assessment.model;

/** Cycle de vie d'une évaluation. */
public enum AssessmentStatus {
    /** En cours de saisie : les réponses et les cibles sont modifiables. */
    DRAFT,
    /** Soumise : les réponses sont verrouillées et le score global est figé. */
    SUBMITTED
}

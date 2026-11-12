package ma.uca.aima.assessment.model;

/** Décision de l'auditeur sur une réponse. */
public enum ReviewStatus {
    /** Pas encore revue. */
    PENDING,
    /** L'auditeur confirme la réponse de l'organisation. */
    CONFIRMED,
    /** L'auditeur retient un autre niveau (au vu des preuves). */
    ADJUSTED
}

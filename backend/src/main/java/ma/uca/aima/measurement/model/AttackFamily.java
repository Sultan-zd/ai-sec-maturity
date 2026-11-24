package ma.uca.aima.measurement.model;

import java.util.Set;

/**
 * Familles d'attaques du labo red-team et questions du référentiel qu'elles mesurent.
 * <p>
 * Cette correspondance est le contrat entre le labo et la plateforme : une mesure
 * « injection de prompt » ne peut porter que sur LLM.1.1, etc. {@link #OTHER} laisse
 * passer toute question (attaque non prévue par la liste, à documenter dans la preuve).
 */
public enum AttackFamily {

    /** Injection de prompt directe ou indirecte (OWASP LLM01). */
    PROMPT_INJECTION("Injection de prompt", Set.of("LLM.1.1")),
    /** Extraction du prompt système et de ses secrets. */
    PROMPT_LEAKING("Fuite du prompt système", Set.of("LLM.1.2")),
    /** Divulgation de données sensibles par le modèle. */
    DATA_DISCLOSURE("Divulgation de données", Set.of("LLM.2.2")),
    /** Utilisation dangereuse d'outils : permissions excessives, action sensible sans validation humaine. */
    UNSAFE_TOOL_USE("Utilisation dangereuse d'outils", Set.of("LLM.3.1", "LLM.3.2")),
    /** Accès à des documents RAG d'un autre utilisateur. */
    RAG_ACCESS_BREAK("Contournement des droits RAG", Set.of("LLM.4.1")),
    /** Autre attaque : toute question est acceptée. */
    OTHER("Autre", Set.of());

    private final String label;
    private final Set<String> questionCodes;

    AttackFamily(String label, Set<String> questionCodes) {
        this.label = label;
        this.questionCodes = questionCodes;
    }

    public String label() {
        return label;
    }

    public Set<String> questionCodes() {
        return questionCodes;
    }

    public boolean measures(String questionCode) {
        return this == OTHER || questionCodes.contains(questionCode);
    }
}

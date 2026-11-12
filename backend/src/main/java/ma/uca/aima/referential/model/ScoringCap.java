package ma.uca.aima.referential.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Règle du « maillon faible » : si une question de criticité {@code criticality}
 * est au niveau {@code answerLevel}, le score de son domaine est plafonné à {@code domainCap}.
 */
@Entity
@Table(name = "ref_scoring_cap")
public class ScoringCap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referential_id")
    private Referential referential;

    @Column(nullable = false)
    private Integer criticality;

    @Column(name = "answer_level", nullable = false)
    private Integer answerLevel;

    @Column(name = "domain_cap", nullable = false, precision = 3, scale = 2)
    private BigDecimal domainCap;

    protected ScoringCap() {
    }

    public ScoringCap(int criticality, int answerLevel, BigDecimal domainCap) {
        this.criticality = criticality;
        this.answerLevel = answerLevel;
        this.domainCap = domainCap;
    }

    void setReferential(Referential referential) { this.referential = referential; }

    public Long getId() { return id; }
    public int getCriticality() { return criticality; }
    public int getAnswerLevel() { return answerLevel; }
    public BigDecimal getDomainCap() { return domainCap; }
}

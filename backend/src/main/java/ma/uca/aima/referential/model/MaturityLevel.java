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

/** Un des 5 niveaux de maturité (Initial, Reproductible, Défini, Maîtrisé, Optimisé). */
@Entity
@Table(name = "ref_maturity_level")
public class MaturityLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referential_id")
    private Referential referential;

    @Column(nullable = false)
    private Integer level;

    @Column(nullable = false, length = 100)
    private String name;

    /** Score minimal (inclus) à partir duquel ce niveau est atteint. */
    @Column(name = "min_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal minScore;

    @Column(nullable = false, length = 1000)
    private String description;

    protected MaturityLevel() {
    }

    public MaturityLevel(int level, String name, BigDecimal minScore, String description) {
        this.level = level;
        this.name = name;
        this.minScore = minScore;
        this.description = description;
    }

    void setReferential(Referential referential) { this.referential = referential; }

    public Long getId() { return id; }
    public int getLevel() { return level; }
    public String getName() { return name; }
    public BigDecimal getMinScore() { return minScore; }
    public String getDescription() { return description; }
}

package ma.uca.aima.referential.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/** Question de la grille d'audit, avec ses 5 niveaux de réponse. */
@Entity
@Table(name = "ref_question")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subdomain_id")
    private Subdomain subdomain;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 1000)
    private String text;

    /** Preuves attendues par l'auditeur. */
    @Column(nullable = false, length = 1000)
    private String evidence;

    /** 1 = normale, 2 = importante, 3 = critique. */
    @Column(nullable = false)
    private Integer criticality;

    /** Poids dans la moyenne du sous-domaine (dérivé de la criticité). */
    @Column(nullable = false)
    private Integer weight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Effort effort;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("level")
    private List<QuestionLevel> levels = new ArrayList<>();

    protected Question() {
    }

    public Question(String code, String text, String evidence, int criticality, int weight,
                    Effort effort, int displayOrder) {
        this.code = code;
        this.text = text;
        this.evidence = evidence;
        this.criticality = criticality;
        this.weight = weight;
        this.effort = effort;
        this.displayOrder = displayOrder;
    }

    public void addLevel(QuestionLevel level) {
        level.setQuestion(this);
        levels.add(level);
    }

    void setSubdomain(Subdomain subdomain) { this.subdomain = subdomain; }

    public Long getId() { return id; }
    public Subdomain getSubdomain() { return subdomain; }
    public String getCode() { return code; }
    public String getText() { return text; }
    public String getEvidence() { return evidence; }
    public int getCriticality() { return criticality; }
    public int getWeight() { return weight; }
    public Effort getEffort() { return effort; }
    public int getDisplayOrder() { return displayOrder; }
    public List<QuestionLevel> getLevels() { return levels; }
}

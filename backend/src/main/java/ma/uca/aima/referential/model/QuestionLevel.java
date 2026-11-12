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

/**
 * Réponse possible à une question : description de la situation au niveau {@code level}
 * et recommandation pour atteindre le niveau suivant (absente au niveau 5).
 */
@Entity
@Table(name = "ref_question_level")
public class QuestionLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(nullable = false)
    private Integer level;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(length = 1000)
    private String recommendation;

    protected QuestionLevel() {
    }

    public QuestionLevel(int level, String description, String recommendation) {
        this.level = level;
        this.description = description;
        this.recommendation = recommendation;
    }

    void setQuestion(Question question) { this.question = question; }

    public Long getId() { return id; }
    public int getLevel() { return level; }
    public String getDescription() { return description; }
    public String getRecommendation() { return recommendation; }
}

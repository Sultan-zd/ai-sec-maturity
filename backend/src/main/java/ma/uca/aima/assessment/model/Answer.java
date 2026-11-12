package ma.uca.aima.assessment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import ma.uca.aima.referential.model.Question;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Réponse d'une organisation à une question du référentiel, dans le cadre d'une évaluation. */
@Entity
@Table(name = "answer")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id")
    private Assessment assessment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    /** Niveau 1 à 5, null si la question est « Non applicable ». */
    private Integer level;

    @Column(name = "not_applicable", nullable = false)
    private boolean notApplicable;

    @Column(length = 1000)
    private String justification;

    @Column(length = 2000)
    private String comment;

    /** Référence de la preuve (lien, nom de document). Une réponse avec preuve est dite « prouvée ». */
    @Column(length = 1000)
    private String evidence;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected Answer() {
    }

    public Answer(Assessment assessment, Question question) {
        this.assessment = assessment;
        this.question = question;
    }

    public void update(Integer level, boolean notApplicable, String justification, String comment,
                       String evidence) {
        this.level = notApplicable ? null : level;
        this.notApplicable = notApplicable;
        this.justification = blankToNull(justification);
        this.comment = blankToNull(comment);
        this.evidence = blankToNull(evidence);
    }

    public boolean hasEvidence() {
        return evidence != null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() { return id; }
    public Assessment getAssessment() { return assessment; }
    public Question getQuestion() { return question; }
    public Integer getLevel() { return level; }
    public boolean isNotApplicable() { return notApplicable; }
    public String getJustification() { return justification; }
    public String getComment() { return comment; }
    public String getEvidence() { return evidence; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

package ma.uca.aima.assessment.model;

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

    // ---- Revue par l'auditeur
    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    private ReviewStatus reviewStatus = ReviewStatus.PENDING;

    /** Niveau retenu par l'auditeur quand il corrige la réponse (ADJUSTED). */
    @Column(name = "audited_level")
    private Integer auditedLevel;

    @Column(name = "auditor_comment", length = 2000)
    private String auditorComment;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Version
    private Long version;

    protected Answer() {
    }

    public Answer(Assessment assessment, Question question) {
        this.assessment = assessment;
        this.question = question;
    }

    /** Saisie par l'organisation. Toute modification annule une éventuelle revue précédente. */
    public void update(Integer level, boolean notApplicable, String justification, String comment,
                       String evidence) {
        this.level = notApplicable ? null : level;
        this.notApplicable = notApplicable;
        this.justification = blankToNull(justification);
        this.comment = blankToNull(comment);
        this.evidence = blankToNull(evidence);
        resetReview();
    }

    /** Décision de l'auditeur : confirmer la réponse, ou retenir un autre niveau. */
    public void review(ReviewStatus decision, Integer auditedLevel, String auditorComment, UUID auditorId) {
        this.reviewStatus = decision;
        this.auditedLevel = decision == ReviewStatus.ADJUSTED ? auditedLevel : null;
        this.auditorComment = blankToNull(auditorComment);
        this.reviewedBy = auditorId;
        this.reviewedAt = Instant.now();
    }

    public void resetReview() {
        this.reviewStatus = ReviewStatus.PENDING;
        this.auditedLevel = null;
        this.auditorComment = null;
        this.reviewedBy = null;
        this.reviewedAt = null;
    }

    /**
     * Niveau pris en compte dans le calcul : le niveau corrigé par l'auditeur s'il existe,
     * sinon le niveau déclaré (null si « Non applicable » non contesté).
     */
    public Integer effectiveLevel() {
        return reviewStatus == ReviewStatus.ADJUSTED ? auditedLevel : level;
    }

    public boolean effectivelyNotApplicable() {
        return reviewStatus != ReviewStatus.ADJUSTED && notApplicable;
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
    public ReviewStatus getReviewStatus() { return reviewStatus; }
    public Integer getAuditedLevel() { return auditedLevel; }
    public String getAuditorComment() { return auditorComment; }
    public UUID getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
}

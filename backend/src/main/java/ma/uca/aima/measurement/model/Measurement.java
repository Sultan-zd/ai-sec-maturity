package ma.uca.aima.measurement.model;

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
import ma.uca.aima.assessment.model.Assessment;
import ma.uca.aima.referential.model.Question;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Niveau mesuré par un test technique (labo red-team) pour une question d'une évaluation.
 * Dans le calcul, une mesure prime sur la réponse déclarée et sur la revue de l'auditeur :
 * c'est la source la plus objective (mesuré &gt; audité &gt; déclaré).
 */
@Entity
@Table(name = "measurement")
public class Measurement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id")
    private Assessment assessment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(name = "measured_level", nullable = false)
    private int measuredLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "attack_family", nullable = false, length = 40)
    private AttackFamily attackFamily;

    @Column(nullable = false, length = 100)
    private String tool;

    @Column(name = "run_id", nullable = false, length = 100)
    private String runId;

    @Column(nullable = false, length = 2000)
    private String evidence;

    @Column(name = "evidence_url", length = 1000)
    private String evidenceUrl;

    @Column(name = "measured_at", nullable = false)
    private Instant measuredAt;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected Measurement() {
    }

    public Measurement(Assessment assessment, Question question) {
        this.assessment = assessment;
        this.question = question;
    }

    /** Enregistre (ou remplace) le résultat d'un passage de tests. */
    public void record(int measuredLevel, AttackFamily attackFamily, String tool, String runId, String evidence,
                       String evidenceUrl, Instant measuredAt, UUID recordedBy) {
        this.measuredLevel = measuredLevel;
        this.attackFamily = attackFamily;
        this.tool = tool;
        this.runId = runId;
        this.evidence = evidence;
        this.evidenceUrl = evidenceUrl == null || evidenceUrl.isBlank() ? null : evidenceUrl.trim();
        this.measuredAt = measuredAt;
        this.recordedBy = recordedBy;
    }

    public UUID getId() { return id; }
    public Assessment getAssessment() { return assessment; }
    public Question getQuestion() { return question; }
    public int getMeasuredLevel() { return measuredLevel; }
    public AttackFamily getAttackFamily() { return attackFamily; }
    public String getTool() { return tool; }
    public String getRunId() { return runId; }
    public String getEvidence() { return evidence; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public Instant getMeasuredAt() { return measuredAt; }
    public UUID getRecordedBy() { return recordedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
}

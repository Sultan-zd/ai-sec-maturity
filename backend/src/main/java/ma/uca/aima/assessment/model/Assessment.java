package ma.uca.aima.assessment.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import ma.uca.aima.common.BusinessRuleException;
import ma.uca.aima.organization.model.Organization;
import ma.uca.aima.referential.model.Referential;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Une évaluation de maturité d'une organisation, réalisée avec une version précise du référentiel.
 * Les règles de cycle de vie (qui peut modifier quoi et quand) sont portées par l'entité elle-même.
 */
@Entity
@Table(name = "assessment")
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referential_id")
    private Referential referential;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String scope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssessmentStatus status = AssessmentStatus.DRAFT;

    /** Niveau cible par code de domaine. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "assessment_target", joinColumns = @JoinColumn(name = "assessment_id"))
    @MapKeyColumn(name = "domain_code", length = 20)
    @Column(name = "target_level", nullable = false)
    private Map<String, Integer> targets = new HashMap<>();

    @Column(name = "global_score", precision = 3, scale = 2)
    private BigDecimal globalScore;

    @Column(name = "global_level")
    private Integer globalLevel;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    /** Traçabilité : qui a créé l'évaluation, qui l'a validée et quand. */
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "validated_by")
    private UUID validatedBy;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Version
    private Long version;

    protected Assessment() {
    }

    public Assessment(Organization organization, Referential referential, String title, String scope,
                      Map<String, Integer> targets, UUID createdBy) {
        this.organization = organization;
        this.referential = referential;
        this.title = title;
        this.scope = scope;
        this.targets.putAll(targets);
        this.createdBy = createdBy;
    }

    /** Lève une erreur 409 si l'évaluation n'est pas en attente de revue par un auditeur. */
    public void assertUnderReview() {
        if (status != AssessmentStatus.SUBMITTED) {
            throw BusinessRuleException.conflict("Seule une évaluation soumise peut être auditée");
        }
    }

    /** Validation finale par l'auditeur : le score retenu est celui calculé avec les niveaux audités. */
    public void validate(BigDecimal auditedScore, int auditedLevel, UUID auditorId) {
        assertUnderReview();
        this.status = AssessmentStatus.VALIDATED;
        this.globalScore = auditedScore;
        this.globalLevel = auditedLevel;
        this.validatedBy = auditorId;
        this.validatedAt = Instant.now();
    }

    /** Lève une erreur 409 si l'évaluation n'est plus modifiable. */
    public void assertEditable() {
        if (status != AssessmentStatus.DRAFT) {
            throw BusinessRuleException.conflict(status == AssessmentStatus.VALIDATED
                    ? "L'évaluation est validée par un auditeur : elle n'est plus modifiable"
                    : "L'évaluation est soumise : rouvrez-la avant de modifier les réponses ou les cibles");
        }
    }

    public void updateTargets(Map<String, Integer> newTargets) {
        assertEditable();
        targets.putAll(newTargets);
    }

    public void submit(BigDecimal score, int level) {
        assertEditable();
        this.status = AssessmentStatus.SUBMITTED;
        this.globalScore = score;
        this.globalLevel = level;
        this.submittedAt = Instant.now();
    }

    public void reopen() {
        if (status != AssessmentStatus.SUBMITTED) {
            throw BusinessRuleException.conflict(
                    "Seule une évaluation soumise (non encore validée) peut être rouverte");
        }
        this.status = AssessmentStatus.DRAFT;
        this.globalScore = null;
        this.globalLevel = null;
        this.submittedAt = null;
    }

    public UUID getId() { return id; }
    public Organization getOrganization() { return organization; }
    public Referential getReferential() { return referential; }
    public String getTitle() { return title; }
    public String getScope() { return scope; }
    public AssessmentStatus getStatus() { return status; }
    public Map<String, Integer> getTargets() { return Map.copyOf(targets); }
    public BigDecimal getGlobalScore() { return globalScore; }
    public Integer getGlobalLevel() { return globalLevel; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getValidatedBy() { return validatedBy; }
    public Instant getValidatedAt() { return validatedAt; }
}

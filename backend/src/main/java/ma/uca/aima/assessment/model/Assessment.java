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

    @Version
    private Long version;

    protected Assessment() {
    }

    public Assessment(Organization organization, Referential referential, String title, String scope,
                      Map<String, Integer> targets) {
        this.organization = organization;
        this.referential = referential;
        this.title = title;
        this.scope = scope;
        this.targets.putAll(targets);
    }

    /** Lève une erreur 409 si l'évaluation n'est plus modifiable. */
    public void assertEditable() {
        if (status != AssessmentStatus.DRAFT) {
            throw BusinessRuleException.conflict(
                    "L'évaluation est soumise : rouvrez-la avant de modifier les réponses ou les cibles");
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
            throw BusinessRuleException.conflict("Seule une évaluation soumise peut être rouverte");
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
}

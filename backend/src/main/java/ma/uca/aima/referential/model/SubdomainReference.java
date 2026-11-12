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

/** Correspondance entre un sous-domaine et une exigence d'une norme (ex. ISO42001 → A.6.2.4). */
@Entity
@Table(name = "ref_subdomain_reference")
public class SubdomainReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subdomain_id")
    private Subdomain subdomain;

    @Column(name = "source_code", nullable = false, length = 50)
    private String sourceCode;

    @Column(nullable = false, length = 100)
    private String clause;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    protected SubdomainReference() {
    }

    public SubdomainReference(String sourceCode, String clause, int displayOrder) {
        this.sourceCode = sourceCode;
        this.clause = clause;
        this.displayOrder = displayOrder;
    }

    void setSubdomain(Subdomain subdomain) { this.subdomain = subdomain; }

    public Long getId() { return id; }
    public String getSourceCode() { return sourceCode; }
    public String getClause() { return clause; }
    public int getDisplayOrder() { return displayOrder; }
}

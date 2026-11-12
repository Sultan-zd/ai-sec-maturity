package ma.uca.aima.referential.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Domaine de sécurité évalué (ex. GOV — Gouvernance & stratégie IA). */
@Entity
@Table(name = "ref_domain")
public class Domain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referential_id")
    private Referential referential;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    /** Poids du domaine dans le score global (la somme des poids vaut 1). */
    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal weight;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @OneToMany(mappedBy = "domain", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder")
    private List<Subdomain> subdomains = new ArrayList<>();

    protected Domain() {
    }

    public Domain(String code, String name, String description, BigDecimal weight, int displayOrder) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.weight = weight;
        this.displayOrder = displayOrder;
    }

    public void addSubdomain(Subdomain subdomain) {
        subdomain.setDomain(this);
        subdomains.add(subdomain);
    }

    void setReferential(Referential referential) { this.referential = referential; }

    public Long getId() { return id; }
    public Referential getReferential() { return referential; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getWeight() { return weight; }
    public int getDisplayOrder() { return displayOrder; }
    public List<Subdomain> getSubdomains() { return subdomains; }
}

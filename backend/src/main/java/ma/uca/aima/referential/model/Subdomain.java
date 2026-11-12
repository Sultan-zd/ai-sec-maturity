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

/** Sous-domaine (ex. LLM.1 — Injection de prompt et manipulation des entrées). */
@Entity
@Table(name = "ref_subdomain")
public class Subdomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "domain_id")
    private Domain domain;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal weight = BigDecimal.ONE;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @OneToMany(mappedBy = "subdomain", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder")
    private List<SubdomainReference> references = new ArrayList<>();

    @OneToMany(mappedBy = "subdomain", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder")
    private List<Question> questions = new ArrayList<>();

    protected Subdomain() {
    }

    public Subdomain(String code, String name, String description, int displayOrder) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    public void addReference(SubdomainReference reference) {
        reference.setSubdomain(this);
        references.add(reference);
    }

    public void addQuestion(Question question) {
        question.setSubdomain(this);
        questions.add(question);
    }

    void setDomain(Domain domain) { this.domain = domain; }

    public Long getId() { return id; }
    public Domain getDomain() { return domain; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getWeight() { return weight; }
    public int getDisplayOrder() { return displayOrder; }
    public List<SubdomainReference> getReferences() { return references; }
    public List<Question> getQuestions() { return questions; }
}

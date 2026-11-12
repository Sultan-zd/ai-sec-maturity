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

/** Norme ou cadre source du référentiel (ISO/IEC 42001, NIST AI RMF, OWASP…). */
@Entity
@Table(name = "ref_source")
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referential_id")
    private Referential referential;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    protected Source() {
    }

    public Source(String code, String name) {
        this.code = code;
        this.name = name;
    }

    void setReferential(Referential referential) { this.referential = referential; }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
}

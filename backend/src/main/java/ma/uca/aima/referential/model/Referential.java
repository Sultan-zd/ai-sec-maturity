package ma.uca.aima.referential.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Racine du référentiel : une version donnée du modèle de maturité AIMA.
 * Une fois importée, une version n'est jamais modifiée (immuabilité).
 */
@Entity
@Table(name = "ref_referential")
public class Referential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "default_target_level", nullable = false)
    private Integer defaultTargetLevel;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    @OneToMany(mappedBy = "referential", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("code")
    private List<Source> sources = new ArrayList<>();

    @OneToMany(mappedBy = "referential", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("level")
    private List<MaturityLevel> maturityLevels = new ArrayList<>();

    @OneToMany(mappedBy = "referential", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("criticality, answerLevel")
    private List<ScoringCap> scoringCaps = new ArrayList<>();

    @OneToMany(mappedBy = "referential", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder")
    private List<Domain> domains = new ArrayList<>();

    protected Referential() {
        // requis par JPA
    }

    public Referential(String code, String version, String name, String description, int defaultTargetLevel) {
        this.code = code;
        this.version = version;
        this.name = name;
        this.description = description;
        this.defaultTargetLevel = defaultTargetLevel;
        this.importedAt = Instant.now();
    }

    public void addSource(Source source) {
        source.setReferential(this);
        sources.add(source);
    }

    public void addMaturityLevel(MaturityLevel level) {
        level.setReferential(this);
        maturityLevels.add(level);
    }

    public void addScoringCap(ScoringCap cap) {
        cap.setReferential(this);
        scoringCaps.add(cap);
    }

    public void addDomain(Domain domain) {
        domain.setReferential(this);
        domains.add(domain);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getVersion() { return version; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getDefaultTargetLevel() { return defaultTargetLevel; }
    public boolean isActive() { return active; }
    public Instant getImportedAt() { return importedAt; }
    public List<Source> getSources() { return sources; }
    public List<MaturityLevel> getMaturityLevels() { return maturityLevels; }
    public List<ScoringCap> getScoringCaps() { return scoringCaps; }
    public List<Domain> getDomains() { return domains; }
}

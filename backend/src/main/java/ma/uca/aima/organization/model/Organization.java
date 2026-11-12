package ma.uca.aima.organization.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Organisation évaluée (entreprise, administration, établissement…). */
@Entity
@Table(name = "organization")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Sector sector;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_size", nullable = false, length = 10)
    private CompanySize size;

    @Column(length = 100)
    private String country;

    @Column(length = 2000)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Organization() {
    }

    public Organization(String name, Sector sector, CompanySize size, String country, String description) {
        update(name, sector, size, country, description);
    }

    public final void update(String name, Sector sector, CompanySize size, String country, String description) {
        this.name = name;
        this.sector = sector;
        this.size = size;
        this.country = country;
        this.description = description;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public Sector getSector() { return sector; }
    public CompanySize getSize() { return size; }
    public String getCountry() { return country; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

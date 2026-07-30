package com.datacom.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.regex.Pattern;

@Entity
@Table(name = "products")
public class Product {

    private static final Pattern REFERENCE_FORMAT = Pattern.compile("^[A-Z0-9][A-Z0-9-]{2,31}$");

    public static final int MAX_NAME = 150;

    public static final int MAX_DESCRIPTION = 2000;
    public static final int MAX_CATEGORY = 80;
    public static final int MAX_SUBCATEGORY = 80;
    public static final int MAX_MANUFACTURER = 150;
    public static final int MAX_LOT_NUMBER = 50;
    public static final int MAX_CERTIFICATION = 100;
    public static final int MAX_COMMENT = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 32)
    private String reference;

    @Column(length = 150)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(length = 80)
    private String category;

    @Column(length = 80)
    private String subcategory;

    @Column(length = 150)
    private String manufacturer;

    @Column(length = 2)
    private String country;

    @Column(name = "lot_number", length = 50)
    private String lotNumber;

    @Column(length = 100)
    private String certification;

    @Column(name = "author_comment", length = 1000)
    private String authorComment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status = ProductStatus.DRAFT;

    @Column(name = "current_step", nullable = false)
    private short currentStep = 1;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @Column(name = "validated_by")
    private Long validatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "search_text", insertable = false, updatable = false)
    private String searchText;

    protected Product() {
        // JPA
    }

    public Product(Long createdBy) {
        this.createdBy = createdBy;
    }

    public void updateIdentification(String reference, String name, String description) {
        ensureEditable();
        if (reference != null
                && !reference.isBlank()
                && !REFERENCE_FORMAT.matcher(reference).matches()) {
            throw new InvalidReferenceFormatException(reference);
        }
        ensureMaxLength(name, MAX_NAME, "name", "Nom");
        ensureMaxLength(description, MAX_DESCRIPTION, "description", "Description");
        this.reference = reference;
        this.name = name;
        this.description = description;
        touch();
    }

    public void updateClassification(
            String category, String subcategory, String manufacturer, String country) {
        ensureEditable();
        if (country != null && !country.isBlank() && !Countries.isValid(country)) {
            throw new InvalidCountryException(country);
        }
        ensureMaxLength(category, MAX_CATEGORY, "category", "Categorie");
        ensureMaxLength(subcategory, MAX_SUBCATEGORY, "subcategory", "Sous-categorie");
        ensureMaxLength(manufacturer, MAX_MANUFACTURER, "manufacturer", "Fabricant");
        this.category = category;
        this.subcategory = subcategory;
        this.manufacturer = manufacturer;
        this.country = country;
        touch();
    }

    public void updateTraceability(String lotNumber, String certification, String authorComment) {
        ensureEditable();
        ensureMaxLength(lotNumber, MAX_LOT_NUMBER, "lotNumber", "Numero de lot");
        ensureMaxLength(certification, MAX_CERTIFICATION, "certification", "Certification");
        ensureMaxLength(authorComment, MAX_COMMENT, "authorComment", "Commentaire");
        this.lotNumber = lotNumber;
        this.certification = certification;
        this.authorComment = authorComment;
        touch();
    }

    public void moveToStep(int step) {
        ensureEditable();
        if (step < 1 || step > 4) {
            throw new IllegalArgumentException("Etape invalide : " + step);
        }
        this.currentStep = (short) step;
        touch();
    }

    public boolean isComplete() {
        return isNotBlank(reference)
                && isNotBlank(name)
                && isNotBlank(category)
                && isNotBlank(manufacturer)
                && isNotBlank(country)
                && isNotBlank(lotNumber)
                && isNotBlank(certification);
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static void ensureMaxLength(String value, int max, String field, String label) {
        if (value != null && value.length() > max) {
            throw new FieldTooLongException(field, label, max);
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    private void ensureEditable() {
        if (status != ProductStatus.DRAFT) {
            throw new ProductNotEditableException(status);
        }
    }

    public void ensureAuthoredBy(Long actingUserId) {
        if (!createdBy.equals(actingUserId)) {
            throw new UnauthorizedProductActionException(
                    "Seul l'auteur de la fiche peut la consulter et la modifier en brouillon.");
        }
    }

    public void submit(Long actingUserId, Instant now) {
        if (status != ProductStatus.DRAFT) {
            throw new InvalidProductTransitionException("soumettre", status);
        }
        if (!createdBy.equals(actingUserId)) {
            throw new UnauthorizedProductActionException(
                    "Seul l'auteur de la fiche peut la soumettre au controle.");
        }
        if (!isComplete()) {
            throw new IncompleteProductException();
        }
        status = ProductStatus.IN_REVIEW;
        submittedAt = now;
        touch();
    }

    public void validate(Long actingUserId, Instant now) {
        if (status != ProductStatus.IN_REVIEW) {
            throw new InvalidProductTransitionException("valider", status);
        }
        if (createdBy.equals(actingUserId)) {
            throw new UnauthorizedProductActionException(
                    "RG-02 : l'auteur d'une fiche ne peut pas la valider lui-meme.");
        }
        status = ProductStatus.VALIDATED;
        validatedBy = actingUserId;
        validatedAt = now;
        touch();
    }

    public void returnToDraft(Long actingUserId) {
        if (status != ProductStatus.IN_REVIEW) {
            throw new InvalidProductTransitionException("renvoyer en brouillon", status);
        }
        if (createdBy.equals(actingUserId)) {
            throw new UnauthorizedProductActionException(
                    "RG-02 : l'auteur d'une fiche ne peut pas la renvoyer en brouillon lui-meme.");
        }
        status = ProductStatus.DRAFT;
        touch();
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getCountry() {
        return country;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public String getCertification() {
        return certification;
    }

    public String getAuthorComment() {
        return authorComment;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public short getCurrentStep() {
        return currentStep;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Long getValidatedBy() {
        return validatedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getValidatedAt() {
        return validatedAt;
    }

    public long getVersion() {
        return version;
    }
}

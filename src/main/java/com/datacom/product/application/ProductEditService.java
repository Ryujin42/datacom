package com.datacom.product.application;

import com.datacom.audit.domain.AuditAction;
import com.datacom.audit.domain.AuditEntry;
import com.datacom.audit.infrastructure.AuditEntryRepository;
import com.datacom.product.domain.Product;
import com.datacom.product.infrastructure.ProductRepository;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-05 a US-07 : creation d'une fiche et saisie de ses etapes.
 *
 * <p>SEC-02 : seul un OPERATOR atteint ces methodes, et {@link Product#ensureAuthoredBy} y ajoute
 * le controle d'identite de RG-01 — un operateur ne modifie que ses propres brouillons. Toutes les
 * regles de validite (format de reference, liste fermee de pays, longueurs) vivent dans l'entite,
 * si bien qu'une requete forgee contournant l'ecran se heurte aux memes refus (SEC-03).
 */
@Service
@PreAuthorize("hasRole('OPERATOR')")
public class ProductEditService {

    private final ProductRepository productRepository;
    private final AuditEntryRepository auditEntryRepository;

    public ProductEditService(
            ProductRepository productRepository, AuditEntryRepository auditEntryRepository) {
        this.productRepository = productRepository;
        this.auditEntryRepository = auditEntryRepository;
    }

    /**
     * US-11 CA-2 : le commentaire du dernier renvoi en brouillon, a montrer a l'auteur quand il
     * rouvre sa fiche. Vide s'il n'y a jamais eu de renvoi, ou si le controleur n'a rien ecrit — le
     * commentaire est optionnel.
     */
    @Transactional(readOnly = true)
    public Optional<String> lastReturnComment(Long productId) {
        return auditEntryRepository
                .findFirstByProductIdAndActionOrderByOccurredAtDesc(
                        productId, AuditAction.RETURN_TO_DRAFT)
                .map(AuditEntry::getComment)
                .filter(comment -> !comment.isBlank());
    }

    /** US-05 : la fiche nait en DRAFT, etape 1, rattachee a son auteur. */
    @Transactional
    public Long create(Long authorId) {
        return productRepository.save(new Product(authorId)).getId();
    }

    /** Chargement pour affichage : refuse si l'appelant n'est pas l'auteur (RG-01). */
    @Transactional(readOnly = true)
    public Product findForAuthor(Long productId, Long actingUserId) {
        Product product = getProductOrThrow(productId);
        product.ensureAuthoredBy(actingUserId);
        return product;
    }

    @Transactional
    public void saveIdentification(
            Long productId,
            Long actingUserId,
            long expectedVersion,
            ProductStepData.Identification data) {
        Product product = openForEdit(productId, actingUserId, expectedVersion);
        ensureReferenceIsFree(data.reference(), productId);
        product.updateIdentification(data.reference(), data.name(), data.description());
        productRepository.save(product);
    }

    /**
     * RG-11/US-07 CA-3 : l'unicite est garantie par la base, mais la verifier ici permet de nommer
     * la fiche en conflit dans le message plutot que de laisser remonter une violation de
     * contrainte illisible. La contrainte reste la seule autorite en cas de course entre deux
     * enregistrements.
     */
    private void ensureReferenceIsFree(String reference, Long productId) {
        if (reference == null || reference.isBlank()) {
            return;
        }
        productRepository
                .findByReference(reference)
                .filter(existing -> !existing.getId().equals(productId))
                .ifPresent(
                        existing -> {
                            throw new DuplicateReferenceException(reference, existing.getId());
                        });
    }

    @Transactional
    public void saveClassification(
            Long productId,
            Long actingUserId,
            long expectedVersion,
            ProductStepData.Classification data) {
        Product product = openForEdit(productId, actingUserId, expectedVersion);
        product.updateClassification(
                data.category(), data.subcategory(), data.manufacturer(), data.country());
        productRepository.save(product);
    }

    @Transactional
    public void saveTraceability(
            Long productId,
            Long actingUserId,
            long expectedVersion,
            ProductStepData.Traceability data) {
        Product product = openForEdit(productId, actingUserId, expectedVersion);
        product.updateTraceability(data.lotNumber(), data.certification(), data.authorComment());
        productRepository.save(product);
    }

    /** RG-09 : changer d'etape est une ecriture comme une autre, soumise aux memes controles. */
    @Transactional
    public void moveToStep(Long productId, Long actingUserId, int step) {
        Product product = getProductOrThrow(productId);
        product.ensureAuthoredBy(actingUserId);
        product.moveToStep(step);
        productRepository.save(product);
    }

    /**
     * RG-07 : la version attendue vient du formulaire ouvert par l'utilisateur. La comparer ici,
     * plutot que de se reposer sur le seul @Version de JPA, est ce qui detecte le conflit : le
     * service recharge une instance a jour, donc JPA seul ne verrait aucune divergence.
     */
    private Product openForEdit(Long productId, Long actingUserId, long expectedVersion) {
        Product product = getProductOrThrow(productId);
        product.ensureAuthoredBy(actingUserId);
        if (product.getVersion() != expectedVersion) {
            throw new ProductModifiedConcurrentlyException();
        }
        return product;
    }

    private Product getProductOrThrow(Long productId) {
        return productRepository
                .findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Fiche introuvable : " + productId));
    }
}

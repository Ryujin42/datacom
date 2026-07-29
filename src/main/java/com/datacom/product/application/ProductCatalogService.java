package com.datacom.product.application;

import com.datacom.audit.domain.AuditEntry;
import com.datacom.audit.infrastructure.AuditEntryRepository;
import com.datacom.product.domain.Product;
import com.datacom.product.domain.ProductStatus;
import com.datacom.product.infrastructure.ProductListItem;
import com.datacom.product.infrastructure.ProductRepository;
import com.datacom.user.domain.Role;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-12 a US-14 : consultation et recherche, pour les deux roles.
 *
 * <p>RG-01, et correction de ELEV-4 : la portee depend du role, et elle est decidee ici, pas a
 * l'ecran. Un OPERATOR ne voit que ses fiches — y compris s'il forge l'URL d'une fiche qui ne lui
 * appartient pas, puisque c'est la meme methode qui sert les deux cas.
 */
@Service
@PreAuthorize("isAuthenticated()")
public class ProductCatalogService {

    /** ECO-11 : 20 elements par page. */
    public static final int PAGE_SIZE = 20;

    private static final List<String> SORTABLE = List.of("reference", "name", "updatedAt");

    private final ProductRepository productRepository;
    private final AuditEntryRepository auditEntryRepository;

    public ProductCatalogService(
            ProductRepository productRepository, AuditEntryRepository auditEntryRepository) {
        this.productRepository = productRepository;
        this.auditEntryRepository = auditEntryRepository;
    }

    /** US-12 : un OPERATOR ne voit que ses fiches, un VALIDATOR les voit toutes (RG-01). */
    @Transactional(readOnly = true)
    public Page<ProductListItem> list(ProductScope scope, ProductStatus status, ListOrder order) {
        return productRepository.findList(scope.authorIdOrNull(), status, order.toPageable());
    }

    /**
     * US-14, reserve au VALIDATOR. Le terme est normalise et echappe avant d'atteindre la requete
     * (voir {@link SearchTerm}).
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('VALIDATOR')")
    public Page<ProductListItem> search(String term, ListOrder order) {
        return productRepository.search(SearchTerm.toLikePattern(term), order.toPageable());
    }

    /**
     * US-13 CA-2/CA-3 : un OPERATOR qui vise une fiche dont il n'est pas l'auteur est refuse meme
     * par l'URL (ELEV-4) ; un identifiant inconnu remonte une absence, que la couche web traduit en
     * 404 propre (corrige B1).
     */
    @Transactional(readOnly = true)
    public Product findForReading(Long productId, ProductScope scope) {
        Product product =
                productRepository
                        .findById(productId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Fiche introuvable : " + productId));
        scope.ensureVisible(product);
        return product;
    }

    /** US-13 CA-4 : l'historique des transitions, renvois et commentaires compris. */
    @Transactional(readOnly = true)
    public List<AuditEntry> history(Long productId) {
        return auditEntryRepository.findByProductIdOrderByOccurredAtDesc(productId);
    }

    /**
     * US-18 CA-2 : les decomptes de l'accueil. Deux {@code count} en base, jamais un chargement des
     * listes (ECO-03) — l'accueil doit rester constant quel que soit le volume de fiches.
     */
    @Transactional(readOnly = true)
    public HomeCounts homeCounts(Long userId, Role role) {
        long myDrafts = productRepository.countByCreatedByAndStatus(userId, ProductStatus.DRAFT);
        long awaitingReview =
                role == Role.VALIDATOR
                        ? productRepository.countByStatus(ProductStatus.IN_REVIEW)
                        : 0;
        return new HomeCounts(myDrafts, awaitingReview);
    }

    /** Decomptes affiches sur l'accueil : fiches en cours, fiches a controler. */
    public record HomeCounts(long myDrafts, long awaitingReview) {}

    /** Ce que l'utilisateur courant a le droit de voir (RG-01). */
    public record ProductScope(Long userId, Role role) {

        Long authorIdOrNull() {
            return role == Role.VALIDATOR ? null : userId;
        }

        void ensureVisible(Product product) {
            if (role != Role.VALIDATOR) {
                product.ensureAuthoredBy(userId);
            }
        }
    }

    /**
     * US-12 CA-5 : tri sur reference, nom et date de mise a jour — et sur rien d'autre. Le nom de
     * colonne venant de l'URL, le restreindre a une liste fermee evite qu'un parametre forge ne
     * designe une colonne arbitraire (SEC-03).
     */
    public record ListOrder(int page, String sort, boolean descending) {

        public static ListOrder of(int page, String sort, boolean descending) {
            String safe = SORTABLE.contains(sort) ? sort : "updatedAt";
            return new ListOrder(Math.max(page, 0), safe, descending);
        }

        PageRequest toPageable() {
            Sort.Direction direction = descending ? Sort.Direction.DESC : Sort.Direction.ASC;
            return PageRequest.of(page, PAGE_SIZE, Sort.by(direction, sort));
        }
    }
}

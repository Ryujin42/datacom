package com.datacom.product.infrastructure;

import com.datacom.product.domain.Product;
import com.datacom.product.domain.ProductStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * RG-16 : les fiches ne sont jamais supprimees. Etend {@link Repository}, pas {@code
 * JpaRepository}, pour qu'aucune methode de suppression ne soit meme exposee a l'appelant.
 */
public interface ProductRepository extends Repository<Product, Long> {

    Product save(Product product);

    Optional<Product> findById(Long id);

    Optional<Product> findByReference(String reference);

    /**
     * US-09 : file de controle, les plus anciennes soumissions en premier. La jointure sur l'auteur
     * se fait dans la meme requete — la liste reste donc a une requete quel que soit le nombre de
     * lignes (ECO-03), la ou une lecture de l'auteur ligne par ligne en ferait une par fiche.
     * Bornee par le {@link Pageable} : aucune requete non bornee (ECO-11).
     */
    @Query(
            """
            select new com.datacom.product.infrastructure.ReviewQueueItem(
                p.id, p.reference, p.name, p.manufacturer,
                concat(u.firstname, ' ', u.lastname), p.submittedAt)
            from Product p
            join com.datacom.user.domain.User u on u.id = p.createdBy
            where p.status = :status
            order by p.submittedAt asc
            """)
    Page<ReviewQueueItem> findQueueByStatus(
            @Param("status") ProductStatus status, Pageable pageable);

    /**
     * US-12. {@code authorId} nul signifie « toutes les fiches » : c'est l'appelant qui decide de
     * la portee selon le role (RG-01), et le filtre par etat est optionnel de la meme facon. Le tri
     * et la pagination viennent du {@link Pageable}, donc aucune requete non bornee (ECO-11).
     */
    @Query(
            """
            select new com.datacom.product.infrastructure.ProductListItem(
                p.id, p.reference, p.name, p.status, p.currentStep, p.updatedAt)
            from Product p
            where (:authorId is null or p.createdBy = :authorId)
              and (:status is null or p.status = :status)
            """)
    Page<ProductListItem> findList(
            @Param("authorId") Long authorId,
            @Param("status") ProductStatus status,
            Pageable pageable);

    /**
     * US-14. La comparaison porte sur la colonne generee {@code search_text} (migration V6), donc
     * sur l'index trigramme : pas de parcours sequentiel (CA-3). Le terme arrive deja normalise et
     * echappe par l'appelant — {@code %} et {@code _} y sont litteraux (CA-4, SEC-01).
     */
    @Query(
            """
            select new com.datacom.product.infrastructure.ProductListItem(
                p.id, p.reference, p.name, p.status, p.currentStep, p.updatedAt)
            from Product p
            where p.searchText like :pattern escape '\\'
            """)
    Page<ProductListItem> search(@Param("pattern") String pattern, Pageable pageable);

    /** US-18 CA-2 : decompte pour l'accueil. Un {@code count}, jamais un chargement de la liste. */
    long countByCreatedByAndStatus(Long createdBy, ProductStatus status);

    long countByStatus(ProductStatus status);
}

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
}

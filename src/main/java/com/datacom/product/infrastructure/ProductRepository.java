package com.datacom.product.infrastructure;

import com.datacom.product.domain.Product;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/**
 * RG-16 : les fiches ne sont jamais supprimees. Etend {@link Repository}, pas {@code
 * JpaRepository}, pour qu'aucune methode de suppression ne soit meme exposee a l'appelant.
 */
public interface ProductRepository extends Repository<Product, Long> {

    Product save(Product product);

    Optional<Product> findById(Long id);

    Optional<Product> findByReference(String reference);
}

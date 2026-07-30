package com.datacom.product.infrastructure;

import com.datacom.product.domain.Product;
import com.datacom.product.domain.ProductStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends Repository<Product, Long> {

    Product save(Product product);

    Optional<Product> findById(Long id);

    Optional<Product> findByReference(String reference);

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

    @Query(
            """
            select new com.datacom.product.infrastructure.ProductListItem(
                p.id, p.reference, p.name, p.status, p.currentStep, p.updatedAt)
            from Product p
            where p.searchText like :pattern escape '\\'
            """)
    Page<ProductListItem> search(@Param("pattern") String pattern, Pageable pageable);

    long countByCreatedByAndStatus(Long createdBy, ProductStatus status);

    long countByStatus(ProductStatus status);
}

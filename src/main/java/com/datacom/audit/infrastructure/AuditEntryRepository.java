package com.datacom.audit.infrastructure;

import com.datacom.audit.domain.AuditAction;
import com.datacom.audit.domain.AuditEntry;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface AuditEntryRepository extends Repository<AuditEntry, Long> {

    AuditEntry save(AuditEntry entry);

    List<AuditEntry> findByProductIdOrderByOccurredAtDesc(Long productId);

    Optional<AuditEntry> findFirstByProductIdAndActionOrderByOccurredAtDesc(
            Long productId, AuditAction action);

    @Query(
            """
            select e from AuditEntry e
            where e.userId = :userId
              and (:productId is null or e.productId = :productId)
              and e.occurredAt >= :from
              and e.occurredAt < :to
            order by e.occurredAt desc
            """)
    Page<AuditEntry> findDecisionsOf(
            @Param("userId") Long userId,
            @Param("productId") Long productId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}

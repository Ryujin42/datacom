package com.datacom.audit.infrastructure;

import com.datacom.audit.domain.AuditEntry;
import java.util.List;
import org.springframework.data.repository.Repository;

/**
 * RG-17/US-15 CA-3 : etend {@link Repository}, pas {@code JpaRepository} ni {@code CrudRepository},
 * pour qu'aucune methode de suppression ou de mise a jour ne soit exposee. Le journal d'audit ne se
 * modifie ni ne se supprime.
 */
public interface AuditEntryRepository extends Repository<AuditEntry, Long> {

    AuditEntry save(AuditEntry entry);

    List<AuditEntry> findByProductIdOrderByOccurredAtDesc(Long productId);
}

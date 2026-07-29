package com.datacom.audit.infrastructure;

import com.datacom.audit.domain.AuditAction;
import com.datacom.audit.domain.AuditEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/**
 * RG-17/US-15 CA-3 : etend {@link Repository}, pas {@code JpaRepository} ni {@code CrudRepository},
 * pour qu'aucune methode de suppression ou de mise a jour ne soit exposee. Le journal d'audit ne se
 * modifie ni ne se supprime.
 */
public interface AuditEntryRepository extends Repository<AuditEntry, Long> {

    AuditEntry save(AuditEntry entry);

    List<AuditEntry> findByProductIdOrderByOccurredAtDesc(Long productId);

    /**
     * US-11 CA-2 : le commentaire du dernier renvoi en brouillon, celui que l'auteur doit voir en
     * rouvrant sa fiche. Le journal est la seule source — le commentaire n'est pas recopie sur la
     * fiche, ce qui garantit qu'il reste exactement ce qui a ete journalise (RG-17).
     */
    Optional<AuditEntry> findFirstByProductIdAndActionOrderByOccurredAtDesc(
            Long productId, AuditAction action);
}

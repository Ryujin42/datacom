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

    /**
     * US-16 : les seules decisions du controleur connecte, filtrees eventuellement par fiche et par
     * periode, de la plus recente a la plus ancienne. Une seule requete couvre toutes les
     * combinaisons de filtres : {@code productId} nul signifie « toutes les fiches », et les bornes
     * de periode sont toujours renseignees (voir {@code JournalFilter}), ouvertes ou non.
     */
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

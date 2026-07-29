package com.datacom.audit.application;

import com.datacom.audit.domain.AuditEntry;
import com.datacom.audit.infrastructure.AuditEntryRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-16 : le journal d'audit, vu par le controleur qui l'a alimente.
 *
 * <p>CA-1 : « ses seules decisions ». L'identifiant de l'utilisateur vient de la session, jamais
 * d'un parametre de requete — sans quoi il suffirait de changer un numero dans l'URL pour lire les
 * decisions d'un collegue.
 */
@Service
@PreAuthorize("hasRole('VALIDATOR')")
public class AuditJournalService {

    public static final int PAGE_SIZE = 20;

    private final AuditEntryRepository auditEntryRepository;

    public AuditJournalService(AuditEntryRepository auditEntryRepository) {
        this.auditEntryRepository = auditEntryRepository;
    }

    /** CA-2/CA-3 : filtres optionnels par fiche et par periode, du plus recent au plus ancien. */
    @Transactional(readOnly = true)
    public Page<AuditEntry> myDecisions(Long userId, JournalFilter filter, int page) {
        return auditEntryRepository.findDecisionsOf(
                userId,
                filter.productId(),
                filter.fromInstant(),
                filter.toInstant(),
                PageRequest.of(Math.max(page, 0), PAGE_SIZE));
    }

    /**
     * Bornes saisies comme des dates, converties en instants dans le fuseau du serveur (RG-15). La
     * borne de fin couvre la journee entiere : saisir le meme jour des deux cotes doit rendre les
     * decisions de ce jour-la, ce qu'une comparaison a minuit ne ferait pas.
     *
     * <p>Une borne absente devient une borne extreme plutot qu'un {@code null}. La raison est
     * concrete : compare a un {@code NULL} non type, Postgres refuse la requete faute de pouvoir
     * deduire le type du parametre. Des bornes reelles evitent d'avoir a forcer un {@code cast}
     * dans la requete, et disent la meme chose plus simplement — « depuis toujours », « jusqu'a
     * aujourd'hui ».
     */
    public record JournalFilter(Long productId, LocalDate from, LocalDate to) {

        private static final LocalDate OPEN_START = LocalDate.of(1970, 1, 1);
        private static final LocalDate OPEN_END = LocalDate.of(9999, 12, 31);

        Instant fromInstant() {
            return atStartOfDay(from == null ? OPEN_START : from);
        }

        Instant toInstant() {
            return atStartOfDay((to == null ? OPEN_END : to).plusDays(1));
        }

        private static Instant atStartOfDay(LocalDate date) {
            return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        }
    }
}

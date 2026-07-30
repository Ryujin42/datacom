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

@Service
@PreAuthorize("hasRole('VALIDATOR')")
public class AuditJournalService {

    public static final int PAGE_SIZE = 20;

    private final AuditEntryRepository auditEntryRepository;

    public AuditJournalService(AuditEntryRepository auditEntryRepository) {
        this.auditEntryRepository = auditEntryRepository;
    }

    @Transactional(readOnly = true)
    public Page<AuditEntry> myDecisions(Long userId, JournalFilter filter, int page) {
        return auditEntryRepository.findDecisionsOf(
                userId,
                filter.productId(),
                filter.fromInstant(),
                filter.toInstant(),
                PageRequest.of(Math.max(page, 0), PAGE_SIZE));
    }

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

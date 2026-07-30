package com.datacom.audit.web;

import com.datacom.audit.application.AuditJournalService;
import com.datacom.audit.application.AuditJournalService.JournalFilter;
import com.datacom.user.infrastructure.UserPrincipal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuditJournalController {

    private final AuditJournalService journalService;

    public AuditJournalController(AuditJournalService journalService) {
        this.journalService = journalService;
    }

    @GetMapping("/journal")
    public String journal(
            @RequestParam(required = false) Long fiche,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate du,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate au,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal UserPrincipal principal,
            Model model) {
        JournalFilter filter = new JournalFilter(fiche, du, au);

        model.addAttribute("entrees", journalService.myDecisions(principal.getId(), filter, page));
        model.addAttribute("fiche", fiche);
        model.addAttribute("du", du);
        model.addAttribute("au", au);
        return "audit/journal";
    }
}

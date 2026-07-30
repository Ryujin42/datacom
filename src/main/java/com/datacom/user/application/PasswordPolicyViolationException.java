package com.datacom.user.application;

import java.util.List;

public class PasswordPolicyViolationException extends RuntimeException {

    private final List<String> violations;

    public PasswordPolicyViolationException(List<String> violations) {
        super(String.join(" ", violations));
        this.violations = violations;
    }

    /** Un message par critere non respecte, en francais, pret a afficher. */
    public List<String> getViolations() {
        return violations;
    }
}

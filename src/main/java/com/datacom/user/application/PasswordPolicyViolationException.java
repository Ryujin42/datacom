package com.datacom.user.application;

import java.util.List;

public class PasswordPolicyViolationException extends RuntimeException {

    private final List<String> violations;

    public PasswordPolicyViolationException(List<String> violations) {
        super(String.join(" ", violations));
        this.violations = violations;
    }

    public List<String> getViolations() {
        return violations;
    }
}

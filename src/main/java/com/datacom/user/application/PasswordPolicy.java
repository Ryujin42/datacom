package com.datacom.user.application;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
public class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    private static final String COMMON_PASSWORDS_RESOURCE = "security/common-passwords-12plus.txt";

    private final Set<String> commonPasswords;

    public PasswordPolicy() {
        this.commonPasswords = loadCommonPasswords();
    }

    public List<String> validate(String candidate) {
        List<String> violations = new ArrayList<>();
        if (candidate == null || candidate.length() < MIN_LENGTH) {
            violations.add(
                    "Le mot de passe doit comporter au moins " + MIN_LENGTH + " caractères.");
        }
        if (candidate != null && commonPasswords.contains(candidate.toLowerCase(Locale.ROOT))) {
            violations.add("Ce mot de passe est trop courant, choisissez-en un autre.");
        }
        return violations;
    }

    public boolean isValid(String candidate) {
        return validate(candidate).isEmpty();
    }

    private Set<String> loadCommonPasswords() {
        try (InputStream in = new ClassPathResource(COMMON_PASSWORDS_RESOURCE).getInputStream()) {
            String content = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
            return content.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(line -> line.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new UncheckedIOException("Liste des mots de passe courants introuvable", e);
        }
    }
}

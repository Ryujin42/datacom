package com.datacom.user.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void rejectsPasswordsShorterThanTwelveCharacters() {
        assertThat(policy.isValid("Short1234")).isFalse();
    }

    @Test
    void acceptsALongEnoughUncommonPassword() {
        assertThat(policy.isValid("Xk7!qLp9#vTz2Rw")).isTrue();
    }

    @Test
    void rejectsACommonPasswordEvenIfLongEnough() {
        assertThat(policy.isValid("password1234")).isFalse();
    }

    @Test
    void commonPasswordCheckIsCaseInsensitive() {
        assertThat(policy.isValid("PASSWORD1234")).isFalse();
    }

    @Test
    void reportsBothViolationsWhenBothApply() {
        assertThat(policy.validate("pass")).hasSize(1);
    }
}

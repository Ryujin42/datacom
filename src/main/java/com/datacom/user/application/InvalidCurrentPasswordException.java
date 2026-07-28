package com.datacom.user.application;

public class InvalidCurrentPasswordException extends RuntimeException {

    public InvalidCurrentPasswordException() {
        super("Mot de passe actuel incorrect.");
    }
}

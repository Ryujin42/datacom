package com.datacom.product.web;

import com.datacom.product.domain.UnauthorizedProductActionException;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Un refus d'autorisation donne un 403, pas un formulaire : l'utilisateur n'a rien a corriger.
 *
 * <p>La traduction se fait ici, dans la couche web, et non par une annotation
 * {@code @ResponseStatus} posee sur l'exception : celle-ci vit dans le domaine, qui ne doit rien
 * savoir de HTTP ni de Spring (TEC-01, verifie par ArchitectureTest).
 *
 * <p>SEC-13 : la page 403 est la page applicative en francais, sans trace technique.
 */
@ControllerAdvice
public class ProductExceptionHandler {

    @ExceptionHandler(UnauthorizedProductActionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleUnauthorized() {
        return "error/403";
    }

    /**
     * US-13 CA-3 : un identifiant inexistant donne un 404 applicatif, pas une trace technique. Le
     * legacy affichait une erreur brute dans ce cas (B1, ELEV-5).
     */
    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound() {
        return "error/404";
    }
}

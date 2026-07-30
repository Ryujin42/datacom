package com.datacom.product.web;

import com.datacom.product.domain.UnauthorizedProductActionException;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ProductExceptionHandler {

    @ExceptionHandler(UnauthorizedProductActionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleUnauthorized() {
        return "error/403";
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound() {
        return "error/404";
    }
}

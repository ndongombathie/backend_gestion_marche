package com.marchepro.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String entite, Long id) {
        super(entite + " introuvable (id=" + id + ")", HttpStatus.NOT_FOUND);
    }
}

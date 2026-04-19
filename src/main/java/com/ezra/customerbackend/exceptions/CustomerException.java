package com.ezra.customerbackend.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomerException extends RuntimeException{
    private final HttpStatus status;
    public CustomerException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

}

package com.octopus.demo.admin.exception;

/** Raised when a correct credential pair belongs to a disabled account. */
public class DisabledAccountException extends RuntimeException {

    public DisabledAccountException(String message) {
        super(message);
    }
}

package com.flashsale.exception;

public class DuplicatePurchaseException extends RuntimeException {
    public DuplicatePurchaseException(String message) {
        super(message);
    }
}

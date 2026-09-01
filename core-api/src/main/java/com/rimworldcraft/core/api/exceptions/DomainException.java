package com.rimworldcraft.core.api.exceptions;

/** Base unchecked exception for domain failures. */
public class DomainException extends RuntimeException {
    /** Creates an exception with a message. */
    public DomainException(String message) { super(message); }
    /** Creates an exception with a message and cause. */
    public DomainException(String message, Throwable cause) { super(message, cause); }
}

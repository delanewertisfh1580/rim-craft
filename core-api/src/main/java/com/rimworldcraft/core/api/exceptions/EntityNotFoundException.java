package com.rimworldcraft.core.api.exceptions;

/** Indicates that a requested entity does not exist. */
public class EntityNotFoundException extends DomainException {
    /** Creates the exception. */
    public EntityNotFoundException(String message) { super(message); }
}

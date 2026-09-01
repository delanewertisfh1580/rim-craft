package com.rimworldcraft.core.api.exceptions;
/** Indicates an event could not be published. */
public class EventPublishException extends DomainException { /** Creates the exception. */ public EventPublishException(String message, Throwable cause) { super(message, cause); } }

package com.rimworldcraft.core.api.exceptions;
/** Indicates that a task queue cannot accept more work. */
public class TaskQueueFullException extends DomainException { /** Creates the exception. */ public TaskQueueFullException(String message) { super(message); } }

package com.rimworldcraft.core.events;

@FunctionalInterface
public interface EventHandler { void handle(EventEnvelope event) throws Exception; }
